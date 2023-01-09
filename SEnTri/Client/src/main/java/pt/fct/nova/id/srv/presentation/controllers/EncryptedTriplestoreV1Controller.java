package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ResultSetStream;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingBuilder;
import org.apache.jena.sparql.modify.request.QuadDataAcc;
import org.apache.jena.sparql.modify.request.UpdateDataInsert;
import org.apache.jena.update.UpdateRequest;
import org.json.JSONObject;
import pt.fct.nova.id.srv.application.SPARQLQueryEngine;
import pt.fct.nova.id.srv.application.clients.*;
import pt.fct.nova.id.srv.application.protocols.ProtocolUtils;
import pt.fct.nova.id.srv.application.protocols.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.protocols.Protocol1;
import pt.fct.nova.id.srv.application.query.QueryType;
import pt.fct.nova.id.srv.application.query.QueryUtils;
import pt.fct.nova.id.srv.application.query.execution.SPARQLResult;
import pt.fct.nova.id.srv.application.query.jobs.*;
import pt.fct.nova.id.srv.application.query.plans.DefaultQueryExecutionPlan;
import pt.fct.nova.id.srv.application.query.plans.SecureSPARQLPlanner;
import pt.fct.nova.id.srv.presentation.api.EncryptedTriplestoreAPI;
import pt.fct.nova.id.srv.presentation.api.dtos.QueryForm;
import pt.fct.nova.id.srv.presentation.api.dtos.UploadForm;
import pt.fct.nova.id.srv.presentation.exceptions.UnknownRDFLanguageException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static jakarta.ws.rs.core.Response.Status.*;
import static pt.fct.nova.id.srv.application.query.QueryType.*;
import static pt.fct.nova.id.srv.presentation.controllers.ParsingUtils.*;
import static pt.fct.nova.id.srv.presentation.controllers.TriplestoreController.*;
import static pt.fct.nova.id.srv.presentation.controllers.TriplestoreController.INTERNAL_ERROR;

@Path("triplestores/v1/encrypted")
public class EncryptedTriplestoreV1Controller extends EncryptedTriplestoreController implements EncryptedTriplestoreAPI {
    public static final String SECRETS_KEY = System.getenv("SECRETS_PROTOCOL_KEY");
    public static final String SECRETS_IV = System.getenv("SECRETS_PROTOCOL_IV");
    public static final String SUCCESSFUL_CREATION = "Successful creation.";
    public static final String EMPTY_UPLOAD = "No content to upload.";
    private static final String BAD_NODE = "Data must only contain concrete nodes: IRI, Blank, Literal.";

    @Override
    public Response create(Cookie cookie, UploadForm form) {
        if (cookie == null)
            return Response.ok(INVALID_COOKIE).status(BAD_REQUEST).build();
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            String triplestoreID = form.getTriplestoreID();
            String issuer = form.getIssuer();
            HTTPResponse response = createTriplestoreAccessPolicy(httpClient, cookie, triplestoreID, issuer);
            if (response.getStatus() != OK)
                return response.build();

            response = createAccessToken(httpClient, cookie, issuer, triplestoreID);
            if (response.getStatus() != OK)
                return response.build();
            String accessToken = response.getBody();

            if (form.getContents() == null)
                return Response.ok(SUCCESSFUL_CREATION).build();
            List<Triple> triples = parseTriples(form.getContents(), parseRDFLanguage(form.getSyntax()));

            response = acquireTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
            if (response.getStatus() != OK) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }

            Protocol1 p = new Protocol1();
            response = saveProtocolSecrets(httpClient, triplestoreID, generateSecretsMap(p), accessToken);
            if (response.getStatus() != OK) {
                HTTPResponse response2 = deleteTriplestoreAccessPolicy(httpClient, cookie, triplestoreID, accessToken);
                if (response2.getStatus() != OK)
                    return response2.build();
                releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }

            Collections.shuffle(triples);
            p.exec(triples);

            response = upload(httpClient, triplestoreID, p.getEncryptedNodes(), accessToken);
            releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return response.build();
        } catch (UnknownRDFLanguageException e) {
            e.printStackTrace();
            return Response.ok(INVALID_SYNTAX).status(Response.Status.BAD_REQUEST).build();
        } catch (InvalidNodeException e) {
            e.printStackTrace();
            return Response.ok(BAD_NODE).status(Response.Status.BAD_REQUEST).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response delete(Cookie cookie, String triplestoreID, String issuer) {
        if (cookie == null)
            return Response.ok(INVALID_COOKIE).status(BAD_REQUEST).build();
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            HTTPResponse response = deleteEncryptedTriplestore(httpClient, cookie, triplestoreID, issuer);
            if (response.getStatus() != OK)
                return response.build();
            return Response.ok(SUCCESSFUL_DELETION).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response upload(Cookie cookie, UploadForm form) {
        if (cookie == null)
            return Response.ok(INVALID_COOKIE).status(BAD_REQUEST).build();
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            String issuer = form.getIssuer();
            String triplestoreID = form.getTriplestoreID();

            HTTPResponse response = createAccessToken(httpClient, cookie, issuer, triplestoreID);
            if (response.getStatus() != OK)
                return response.build();
            String accessToken = response.getBody();

            if (form.getContents() == null) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return Response.ok(EMPTY_UPLOAD).status(Response.Status.BAD_REQUEST).build();
            }

            List<Triple> triples = parseTriples(form.getContents(), parseRDFLanguage(form.getSyntax()));
            if (triples.isEmpty()) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return Response.ok(EMPTY_UPLOAD).status(Response.Status.BAD_REQUEST).build();
            }

            response = getProtocolSecrets(httpClient, triplestoreID, accessToken);
            if (response.getStatus() != OK) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }
            Map<String, String> secrets = ParsingUtils.parseMapOfStringString(response.getBody());
            Protocol1 protocol = getProtocol1(secrets);
            Collections.shuffle(triples);
            QuadDataAcc qc = new QuadDataAcc();
            triples.forEach(qc::addTriple);
            String query = new UpdateRequest().add(new UpdateDataInsert(qc)).toString();
            return answerSPARQLQuery(httpClient, cookie, triplestoreID, query, protocol, accessToken);
        } catch (UnknownRDFLanguageException e) {
            return Response.ok(INVALID_SYNTAX).status(Response.Status.BAD_REQUEST).build();
        } catch (InvalidNodeException e) {
            return Response.ok(BAD_NODE).status(Response.Status.BAD_REQUEST).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response answerSPARQLQuery(Cookie cookie, QueryForm form) {
        if (cookie == null)
            return Response.ok(INVALID_COOKIE).status(BAD_REQUEST).build();
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            String triplestoreID = form.getTriplestoreID();
            String issuer = form.getIssuer();

            HTTPResponse response = createAccessToken(httpClient, cookie, issuer, triplestoreID);
            if (response.getStatus() != OK)
                return response.build();
            String accessToken = response.getBody();

            response = getProtocolSecrets(httpClient, triplestoreID, accessToken);
            if (response.getStatus() != OK) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }
            Map<String, String> secrets = ParsingUtils.parseMapOfStringString(response.getBody());
            return answerSPARQLQuery(httpClient, cookie, triplestoreID, form.getQuery(), getProtocol1(secrets), accessToken);
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    private Response answerSPARQLQuery(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID, String query, Protocol1 protocol, String accessToken) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, IOException, URISyntaxException, ClassNotFoundException, InvalidNodeException {
        SecureSPARQLPlanner planner = new SecureSPARQLPlanner();
        DefaultQueryExecutionPlan plan = (DefaultQueryExecutionPlan) new SPARQLQueryEngine(planner).getQueryPlan(query);

        Map<String, Integer> keywordsFrequency = new HashMap<>();
        Set<String> keywords = planner.getKeywords();
        List<String> keywordList = new ArrayList<>(keywords.size());
        List<String> trapdoors = new ArrayList<>(keywords.size());

        for (String keyword : keywords) {
            keywordList.add(keyword);
            trapdoors.add(protocol.generateKeywordsFrequencyTrapdoor(keyword));
        }
        HTTPResponse response = fetchKeywordsFrequency(httpClient, triplestoreID, keywordList, trapdoors, keywordsFrequency, protocol, accessToken);
        if (response != null && response.getStatus() != OK) {
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return response.build();
        }
        QueryType queryType = planner.getQueryType();
        return switch (queryType) {
            case SELECT, ASK, DESCRIBE, CONSTRUCT ->
                    executeSPARQLQuery(httpClient, cookie, triplestoreID, queryType, planner, plan, keywordsFrequency, protocol, accessToken);
            case INSERT_DATA, DELETE_DATA, DELETE_WHERE, MODIFY ->
                    executeSPARQLUpdateQuery(httpClient, cookie, triplestoreID, queryType, planner, plan, keywordsFrequency, protocol, accessToken);
        };
    }

    private HTTPResponse fetchKeywordsFrequency(HttpClient httpClient, String triplestoreID, List<String> keywords, List<String> trapdoors, Map<String, Integer> keywordsFrequencyCollector, Protocol1 protocol, String accessToken) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, IOException {
        HTTPResponse response = searchEncryptedTriplestoreContents(httpClient, triplestoreID, trapdoors, accessToken);
        if (response.getStatus() != OK)
            return response;
        List<String> encryptedKeywordsFrequencies = ParsingUtils.parseListOfStrings(response.getBody());
        String info;
        for (int i = 0; i < encryptedKeywordsFrequencies.size(); i++) {
            info = encryptedKeywordsFrequencies.get(i);
            if (info != null) {
                int total = ProtocolUtils.integerFromByteArray(protocol.decryptRNDLayer(info));
                keywordsFrequencyCollector.put(keywords.get(i), total);
            } else
                keywordsFrequencyCollector.put(keywords.get(i), 0);
        }
        return null;
    }

    private Response executeSPARQLQuery(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID,
                                        QueryType queryType, SecureSPARQLPlanner planner, DefaultQueryExecutionPlan plan,
                                        Map<String, Integer> keywordsFrequency, Protocol1 protocol, String accessToken) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, IOException, URISyntaxException, InvalidKeyException, ClassNotFoundException {
        if (keywordsFrequency.containsValue(0)) {
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return getEmptySPARQLQueryResult(plan, planner.getObfuscationMap());
        }
        HTTPResponse response = prepareSearches(httpClient, protocol, triplestoreID, planner.getSearchJobsIDs(), plan.getJobs(), keywordsFrequency, accessToken);
        if (response != null && response.getStatus() != OK) {
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return response.build();
        }
        response = query(httpClient, protocol.getRNDKey(), plan, accessToken);
        if (response.getStatus() != OK) {
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return response.build();
        }
        SPARQLResult sparqlResult = ParsingUtils.parseSPARQLResult(response.getBody());
        Response res = null;
        if (queryType == SELECT || queryType == CONSTRUCT) {
            Map<Var, Var> obfuscationMap = planner.getObfuscationMap();
            List<Var> vars = new LinkedList<>();
            for (Var var : plan.getVars())
                vars.add(obfuscationMap.get(var));
            Collection<Binding> bindings = decryptBindings(sparqlResult.getBindings(), obfuscationMap, protocol);
            if (sparqlResult.isOrdered())
                bindings = orderResults(sparqlResult.isDistinct(), sparqlResult.getSortConditions(), obfuscationMap, bindings);
            if (sparqlResult.isSliced())
                bindings = sliceResults(sparqlResult.getOffset(), sparqlResult.getLength(), bindings);
            if (queryType == SELECT)
                res = generateSELECTResults(vars, bindings);
            else
                res = generateCONSTRUCTResults(planner.getConstructTemplate(), bindings);
        } else if (queryType == ASK)
            res = generateASKResults(sparqlResult);
        else if (queryType == DESCRIBE)
            res = generateDESCRIBEResults(plan.getVars(), planner.getObfuscationMap(), sparqlResult);
        deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
        return res;
    }


    private Response executeSPARQLUpdateQuery(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID, QueryType queryType, SecureSPARQLPlanner planner,
                                              DefaultQueryExecutionPlan plan, Map<String, Integer> keywordsFrequency, Protocol1 protocol, String accessToken) throws IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, URISyntaxException, InvalidKeyException, ClassNotFoundException, InvalidNodeException {
        HTTPResponse response = acquireTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
        if (response.getStatus() != OK) {
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return response.build();
        }
        protocol.setKeywordsFrequencies(keywordsFrequency);
        Map<String, String> uploads = new HashMap<>();
        Map<String, String> swaps = new HashMap<>();
        Set<String> deletions = new HashSet<>();
        if (queryType == DELETE_WHERE || queryType == MODIFY) {
            response = prepareSearches(httpClient, protocol, triplestoreID, planner.getSearchJobsIDs(), plan.getJobs(), keywordsFrequency, accessToken);
            if (response != null && response.getStatus() != OK) {
                releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }
            response = query(httpClient, protocol.getRNDKey(), plan, accessToken);
            if (response.getStatus() != OK) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }
            SPARQLResult sparqlResult = parseSPARQLResult(response.getBody());
            Map<Var, Var> obfuscationMap = planner.getObfuscationMap();
            Collection<Binding> bindings = decryptBindings(sparqlResult.getBindings(), obfuscationMap, protocol);
            if (sparqlResult.isOrdered())
                bindings = orderResults(sparqlResult.isDistinct(), sparqlResult.getSortConditions(), obfuscationMap, bindings);
            if (sparqlResult.isSliced())
                bindings = sliceResults(sparqlResult.getOffset(), sparqlResult.getLength(), bindings);

            if (bindings.isEmpty()) {
                releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return updateTriplestore(httpClient, cookie, triplestoreID, accessToken, uploads, deletions, swaps);
            }
            List<Triple> triplesToUpload = QueryUtils.generateTriplesFromBindings(planner.getUploadTemplate(), bindings);
            List<Triple> triplesToDelete = QueryUtils.generateTriplesFromBindings(planner.getDeleteTemplate(), bindings);
            triplesToDelete.addAll(triplesToUpload);
            response = computeDeletionsAndSwaps(httpClient, cookie, triplestoreID, protocol, keywordsFrequency, triplesToDelete, swaps, deletions, accessToken);
            if (response != null && response.getStatus() != OK) {
                releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }
            Collections.shuffle(triplesToUpload);
            protocol.exec(triplesToUpload);
            uploads = protocol.getEncryptedNodes();
        } else if (queryType == INSERT_DATA) {
            List<Triple> triples = planner.getUploadTemplate();
            response = computeDeletionsAndSwaps(httpClient, cookie, triplestoreID, protocol, keywordsFrequency, triples, swaps, deletions, accessToken);
            if (response != null && response.getStatus() != OK) {
                releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }
            Collections.shuffle(triples);
            protocol.exec(triples);
            uploads = protocol.getEncryptedNodes();
        } else if (queryType == DELETE_DATA) {
            response = computeDeletionsAndSwaps(httpClient, cookie, triplestoreID, protocol, keywordsFrequency, planner.getDeleteTemplate(), swaps, deletions, accessToken);
            if (response != null && response.getStatus() != OK) {
                releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }
            protocol.exec(new LinkedList<>());
            uploads = protocol.getEncryptedNodes();
        }
        return updateTriplestore(httpClient, cookie, triplestoreID, accessToken, uploads, deletions, swaps);
    }

    private HTTPResponse computeDeletionsAndSwaps(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID, Protocol1 protocol,
                                                  Map<String, Integer> keywordsFrequencyCollector, List<Triple> triplesToDelete,
                                                  Map<String, String> swapsCollector, Set<String> deletionsCollector, String accessToken) throws IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, InvalidNodeException, URISyntaxException {

        Map<String, List<String>> keywordsTrapdoors = protocol.generateKeywordsAndEncryptedValues(triplesToDelete);
        //TODO: Associate keywords with triple pattern trapdoor instances
        Set<String> keywords = new HashSet<>(keywordsTrapdoors.keySet());
        keywords.removeAll(keywordsFrequencyCollector.keySet());
        HTTPResponse response;
        List<String> keywordList;
        List<String> trapdoors;
        if (!keywords.isEmpty()) {
            keywordList = new ArrayList<>(keywords.size());
            trapdoors = new ArrayList<>(keywords.size());
            for (String keyword : keywords) {
                keywordList.add(keyword);
                trapdoors.add(protocol.generateKeywordsFrequencyTrapdoor(keyword));
            }
            response = fetchKeywordsFrequency(httpClient, triplestoreID, keywordList, trapdoors, keywordsFrequencyCollector, protocol, accessToken);
            if (response != null && response.getStatus() != OK)
                return response;
        }
        protocol.setKeywordsFrequencies(keywordsFrequencyCollector);
        keywordList = new ArrayList<>(keywordsTrapdoors.keySet());
        Collections.shuffle(keywordList);
        List<String> trapdoorsToDelete;
        Map<String, List<Integer>> keywordsInstances = new HashMap<>(keywordList.size());
        List<Integer> instances;
        for (String keyword : keywordList) {
            trapdoorsToDelete = keywordsTrapdoors.get(keyword);
            deletionsCollector.addAll(trapdoorsToDelete);
            response = searchEncryptedTriplestoreContents(httpClient, triplestoreID, trapdoorsToDelete, accessToken);
            if (response.getStatus() != OK) {
                releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response;
            }
            instances = ParsingUtils.parseListOfIntegers(response.getBody());
            if (!instances.isEmpty()) {
                keywordsInstances.put(keyword, instances);
                keywordsTrapdoors.put(keyword, new ArrayList<>(protocol.generateTrapdoors(keyword, keywordsInstances.get(keyword))));
            }
        }

        int keywordFrequency, totalInstances, totalSwaps;
        List<Integer> keywordInstances, swapCandidatesInstances;
        List<String> keywordTrapdoors, swapCandidatesTrapdoors;
        for (String keyword : keywordsTrapdoors.keySet()) {
            keywordFrequency = keywordsFrequencyCollector.get(keyword);
            keywordInstances = keywordsInstances.get(keyword);
            keywordTrapdoors = keywordsTrapdoors.get(keyword);
            totalInstances = keywordInstances.size();
            swapCandidatesInstances = new ArrayList<>(keywordFrequency - totalInstances);
            for (int i = keywordFrequency - totalInstances; i < keywordFrequency; i++)
                swapCandidatesInstances.add(i);
            swapCandidatesTrapdoors = protocol.generateTrapdoors(keyword, swapCandidatesInstances);
            totalSwaps = 0;
            for (int i = 0; i < totalInstances; i++) {
                if (keywordInstances.get(i) < keywordFrequency - totalInstances) {
                    swapsCollector.put(keywordTrapdoors.get(i), swapCandidatesTrapdoors.get(totalInstances - totalSwaps));
                    keywordsFrequencyCollector.put(keyword, keywordFrequency - 1);
                    totalSwaps += 1;
                } else {
                    protocol.deleteKeyword(keyword);
                    deletionsCollector.add(keywordTrapdoors.get(i));
                }
            }

        }
        return null;
    }

    private Response updateTriplestore(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID, String accessToken,
                                       Map<String, String> uploads, Set<String> deletions, Map<String, String> swaps) throws IOException {
        JSONObject responseBody = new JSONObject();
        if (!deletions.isEmpty())
            responseBody = responseBody.put("Delete Response:", deleteSomeContents(httpClient, triplestoreID, deletions, accessToken).getBody());
        if (!swaps.isEmpty())
            responseBody = responseBody.put("Swap Response:", swapSomeContents(httpClient, triplestoreID, swaps, accessToken).getBody());
        if (!uploads.isEmpty())
            responseBody = responseBody.put("Insert Response:", upload(httpClient, triplestoreID, uploads, accessToken).getBody());
        releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
        deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
        return Response.ok(responseBody).build();
    }


    private Response generateDESCRIBEResults(List<Var> vars, Map<Var, Var> obfuscationMap, SPARQLResult sparqlResult) {
        Map<Var, Integer> frequencies = new HashMap<>();
        for (Var v : vars)
            frequencies.put(v, 0);
        for (SerializableBinding binding : sparqlResult.getBindings()) {
            for (Iterator<Var> it = binding.vars(); it.hasNext(); ) {
                Var v = it.next();
                frequencies.put(v, frequencies.get(v) + 1);
            }
        }
        JSONObject responseBody = new JSONObject();
        for (Var v : vars)
            responseBody = responseBody.put(obfuscationMap.get(v).getVarName(), frequencies.get(v));
        return Response.ok(responseBody.toString()).build();
    }

    private Response generateCONSTRUCTResults(List<Triple> constructTemplate, Collection<Binding> bindings) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Graph g = QueryUtils.generateGraphFromBindings(constructTemplate, bindings);
            RDFWriter.create(g).lang(Lang.JSONLD).output(out);
            return Response.ok(out.toByteArray()).build();
        }
    }

    private Response generateSELECTResults(List<Var> vars, Collection<Binding> bindings) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ResultSetFormatter.outputAsJSON(out, ResultSetStream.create(vars, bindings.iterator()));
            return Response.ok(out.toByteArray()).build();
        }
    }

    private HTTPResponse prepareSearches(CloseableHttpClient httpClient, Protocol1 protocol,
                                         String triplestoreID, Set<String> jobIDs, Map<String, Job> jobs,
                                         Map<String, Integer> keywordsFrequency, String accessToken) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, IOException, URISyntaxException {
        HTTPResponse response;
        String keyword;
        SecureSearchJob secureSearchJob;
        String[][] trapdoors;
        Var[] vars;
        List<Integer> permutation;
        List<String> searchIDs;
        Map<String, List<String>> searchIDCollector = new HashMap<>();
        int keywordFrequency;
        for (String jobID : jobIDs) {
            secureSearchJob = (SecureSearchJob) jobs.get(jobID);
            vars = secureSearchJob.getVars();
            keyword = secureSearchJob.getSearches().get(vars[0]);
            searchIDs = searchIDCollector.get(keyword);
            if (searchIDs == null) {
                searchIDs = new ArrayList<>(vars.length);
                keywordFrequency = keywordsFrequency.get(keyword);
                permutation = generateRandomPermutation(keywordFrequency / vars.length);
                trapdoors = new String[vars.length][keywordFrequency / vars.length];
                for (int i = 0; i < keywordFrequency / vars.length; i++) {
                    for (int j = 0; j < vars.length; j++)
                        trapdoors[j][permutation.get(i)] = protocol.generateTrapdoor(keyword);
                }
                for (int i = 0; i < vars.length; i++) {
                    response = prepareSearch(httpClient, triplestoreID, List.of(trapdoors[i]), accessToken);
                    if (response.getStatus() != OK)
                        return response;
                    String searchID = response.getBody();
                    searchIDs.add(searchID);
                    secureSearchJob.prepareSearch(vars[i], searchID);
                    System.out.println("[ " + vars[i] + " ] - " + trapdoors[i].length + " | " + searchID + " | " + keyword);
                }
                searchIDCollector.put(keyword, searchIDs);
            } else {
                for (int i = 0; i < vars.length; i++) {
                    secureSearchJob.prepareSearch(vars[i], searchIDs.get(i));
                    System.out.println("[ " + vars[i] + " ] - " + " | " + searchIDs.get(i) + " | " + keyword);
                }
            }
        }
        return null;
    }

    private List<Integer> generateRandomPermutation(int total) {
        List<Integer> idxs = new ArrayList<>(total);
        for (int i = 0; i < total; i++) idxs.add(i);
        Collections.shuffle(idxs);
        return idxs;
    }

    private Collection<Binding> decryptBindings
            (Collection<SerializableBinding> bindings, Map<Var, Var> obfuscationMap, Protocol1 protocol) throws
            InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        Collection<Binding> decryptedBindings = new LinkedList<>();
        BindingBuilder builder = Binding.builder();
        for (SerializableBinding binding : bindings) {
            for (Iterator<Var> it = binding.vars(); it.hasNext(); ) {
                Var var = it.next();
                builder.add(obfuscationMap.get(var), generateNode(new String(protocol.decryptDETLayer(binding.get(var)))));
            }
            decryptedBindings.add(builder.build());
            builder.reset();
        }
        return decryptedBindings;
    }
}
