package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingBuilder;
import org.apache.jena.sparql.modify.request.QuadDataAcc;
import org.apache.jena.sparql.modify.request.UpdateDataInsert;
import org.apache.jena.update.UpdateRequest;
import pt.fct.nova.id.srv.application.SPARQLQueryEngine;
import pt.fct.nova.id.srv.application.clients.*;
import pt.fct.nova.id.srv.application.crypto.SymmetricCipher;
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
    private static final String NO_UPDATES = "No content to update.";

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
        QueryType queryType = planner.getQueryType();
        System.out.println("QUERY TYPE: " + queryType);
        return switch (queryType) {
            case SELECT, ASK, DESCRIBE, CONSTRUCT ->
                    executeSPARQLQuery(httpClient, cookie, triplestoreID, queryType, planner, plan, protocol, accessToken);
            case INSERT_DATA, DELETE_DATA, DELETE_WHERE, MODIFY ->
                    executeSPARQLUpdateQuery(httpClient, cookie, triplestoreID, queryType, planner, plan, protocol, accessToken);
        };
    }

    private HTTPResponse fetchKeywordsFrequencies(HttpClient httpClient, String triplestoreID, List<String> keywords, List<String> trapdoors, Map<String, Integer> keywordsFrequencyCollector, Protocol1 protocol, String accessToken) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, IOException {
        HTTPResponse response = searchEncryptedTriplestoreContents(httpClient, triplestoreID, trapdoors, accessToken);
        if (response.getStatus() != OK)
            return response;
        List<String> encryptedKeywordsFrequencies = ParsingUtils.parseListOfStrings(response.getBody());
        String frequency;
        for (int i = 0; i < encryptedKeywordsFrequencies.size(); i++) {
            frequency = encryptedKeywordsFrequencies.get(i);
            if (frequency != null)
                keywordsFrequencyCollector.put(keywords.get(i), SymmetricCipher.integerFromByteArray(protocol.decryptRNDLayer(frequency)));
            else
                keywordsFrequencyCollector.put(keywords.get(i), 0);
        }
        return null;
    }

    private Response executeSPARQLQuery(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID,
                                        QueryType queryType, SecureSPARQLPlanner planner, DefaultQueryExecutionPlan plan,
                                        Protocol1 protocol, String accessToken) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, IOException, URISyntaxException, InvalidKeyException, ClassNotFoundException {
        Map<String, Integer> keywordsFrequency = new HashMap<>();
        Set<String> keywords = planner.getKeywords();
        List<String> keywordList = new ArrayList<>(keywords.size());
        List<String> trapdoors = new ArrayList<>(keywords.size());

        for (String keyword : keywords) {
            keywordList.add(keyword);
            trapdoors.add(protocol.generateKeywordsFrequencyTrapdoor(keyword));
        }
        HTTPResponse response = fetchKeywordsFrequencies(httpClient, triplestoreID, keywordList, trapdoors, keywordsFrequency, protocol, accessToken);
        if (response != null && response.getStatus() != OK) {
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return response.build();
        }
        if (keywordsFrequency.containsValue(0)) {
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return getEmptySPARQLQueryResult(plan, planner.getObfuscationMap());
        }
        response = prepareSearches(httpClient, protocol, triplestoreID, planner.getSearchJobsIDs(), plan.getJobs(), keywordsFrequency, accessToken);
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
                                              DefaultQueryExecutionPlan plan, Protocol1 protocol, String accessToken) throws IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, URISyntaxException, InvalidKeyException, ClassNotFoundException, InvalidNodeException {
        HTTPResponse response = acquireTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
        if (response.getStatus() != OK) {
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return response.build();
        }
        Map<String, String> swaps = new HashMap<>();
        Set<String> deletions = new HashSet<>();
        List<Triple> triplesToUpload, triplesToDelete;
        Map<String, Integer> keywordsFrequency = new HashMap<>();
        if (queryType == DELETE_WHERE || queryType == MODIFY) {
            Set<String> keywords = planner.getKeywords();
            List<String> keywordList = new ArrayList<>(keywords.size());
            List<String> trapdoors = new ArrayList<>(keywords.size());
            for (String keyword : keywords) {
                keywordList.add(keyword);
                trapdoors.add(protocol.generateKeywordsFrequencyTrapdoor(keyword));
            }
            response = fetchKeywordsFrequencies(httpClient, triplestoreID, keywordList, trapdoors, keywordsFrequency, protocol, accessToken);
            if (response != null && response.getStatus() != OK) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }
            if (keywordsFrequency.containsValue(0)) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                return Response.ok(NO_UPDATES).build();
            }
            response = prepareSearches(httpClient, protocol, triplestoreID, planner.getSearchJobsIDs(), plan.getJobs(), keywordsFrequency, accessToken);
            if (response != null && response.getStatus() != OK) {
                releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }
            response = query(httpClient, protocol.getRNDKey(), plan, accessToken);
            if (response.getStatus() != OK) {
                releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
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
                return Response.ok(NO_UPDATES).build();
            }
            triplesToUpload = QueryUtils.generateTriplesFromBindings(planner.getUploadTemplate(), bindings);
            triplesToDelete = QueryUtils.generateTriplesFromBindings(planner.getDeleteTemplate(), bindings);
            triplesToDelete.addAll(triplesToUpload);
        } else if (queryType == INSERT_DATA) {
            triplesToDelete = planner.getUploadTemplate();
            triplesToUpload = planner.getUploadTemplate();
        } else {
            triplesToDelete = planner.getDeleteTemplate();
            triplesToUpload = planner.getDeleteTemplate();
        }
        Collections.shuffle(triplesToDelete);
        Collections.shuffle(triplesToUpload);
        System.out.println("Triples to Upload: " + triplesToUpload.size());
        System.out.println("Triples to Delete: " + triplesToDelete.size());
        response = computeDeletionsSwapsAndUploads(httpClient, triplestoreID, protocol, keywordsFrequency,
                triplesToDelete, triplesToUpload, swaps, deletions, accessToken);
        if (response != null && response.getStatus() != OK) {
            releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return response.build();
        }
        return updateTriplestore(httpClient, cookie, triplestoreID, protocol.getEncryptedNodes(), deletions, swaps, accessToken);
    }

    private HTTPResponse computeDeletionsSwapsAndUploads(CloseableHttpClient httpClient, String triplestoreID, Protocol1 protocol,
                                                         Map<String, Integer> keywordsFrequency, List<Triple> triplesToDelete, List<Triple> triplesToUpload,
                                                         Map<String, String> swapsCollector, Set<String> deletionsCollector,
                                                         String accessToken) throws IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, InvalidNodeException, URISyntaxException {

        Set<String> keywords = ParsingUtils.generateKeywords(triplesToDelete);
        keywords.removeAll(keywordsFrequency.keySet());
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
            response = fetchKeywordsFrequencies(httpClient, triplestoreID, keywordList, trapdoors, keywordsFrequency, protocol, accessToken);
            if (response != null && response.getStatus() != OK)
                return response;
        }
        protocol.setKeywordsIVs(keywordsFrequency);
        Map<String, List<String>> keywordsTrapdoors = protocol.generateKeywordsPatternTrapdoors(triplesToDelete);
        keywordList = new ArrayList<>(keywordsTrapdoors.keySet());
        List<String> trapdoorsToDelete;
        Map<String, List<Integer>> keywordsInstances = new HashMap<>(keywordList.size());
        List<String> encryptedIVs;
        SortedSet<Integer> instances;
        String encryptedInstance;
        for (String keyword : keywordList) {
            System.out.println("Keyword: " + keyword);
            trapdoorsToDelete = keywordsTrapdoors.get(keyword);
            response = searchEncryptedTriplestoreContents(httpClient, triplestoreID, trapdoorsToDelete, accessToken);
            if (response.getStatus() != OK)
                return response;
            encryptedIVs = ParsingUtils.parseListOfStrings(response.getBody());
            System.out.println("Encrypted: " + encryptedIVs.size());
            if (!encryptedIVs.isEmpty()) {
                instances = new TreeSet<>();
                for (int i = 0; i < encryptedIVs.size(); i++) {
                    encryptedInstance = encryptedIVs.get(i);
                    if (encryptedInstance != null) {
                        instances.add(SymmetricCipher.integerFromByteArray(protocol.decryptRNDLayer(encryptedInstance)));
                        deletionsCollector.add(trapdoorsToDelete.get(i));
                    }
                }
                if (!instances.isEmpty()) {
                    System.out.println("Instances: " + instances.size());
                    keywordsInstances.put(keyword, new ArrayList<>(instances));
                    keywordsTrapdoors.put(keyword, new ArrayList<>(protocol.generateTrapdoors(keyword, keywordsInstances.get(keyword))));
                }
            }
        }
        System.out.println(keywordsInstances.size());
        System.out.println(keywordsTrapdoors.size());
        int keywordFrequency, totalInstances, totalSwaps;
        List<Integer> keywordInstances, swapCandidatesInstances;
        List<String> keywordTrapdoors, swapCandidatesTrapdoors;
        for (String keyword : keywordsTrapdoors.keySet()) {
            keywordFrequency = keywordsFrequency.get(keyword);
            keywordInstances = keywordsInstances.get(keyword);
            keywordTrapdoors = keywordsTrapdoors.get(keyword);

            totalInstances = keywordInstances.size();
            swapCandidatesInstances = new ArrayList<>(keywordFrequency - totalInstances);
            for (int i = keywordFrequency - totalInstances; i < keywordFrequency; i++)
                swapCandidatesInstances.add(i);
            swapCandidatesTrapdoors = protocol.generateTrapdoors(keyword, swapCandidatesInstances);
            System.out.println("[ " + keyword + " ] - " + keywordFrequency + " | " +
                    keywordInstances.size() + " | " + keywordTrapdoors.size() + " | possible swaps: " + swapCandidatesInstances.size());
            totalSwaps = 0;
            for (int i = 0; i < totalInstances; i++) {
                if (keywordInstances.get(i) < keywordFrequency - totalInstances) {
                    swapsCollector.put(swapCandidatesTrapdoors.get(totalInstances - totalSwaps), keywordTrapdoors.get(i));
                    keywordsFrequency.put(keyword, keywordFrequency - 1);
                    totalSwaps += 1;
                } else {
                    protocol.deleteKeyword(keyword);
                    deletionsCollector.add(keywordTrapdoors.get(i));
                }
            }
        }
        System.out.println("Triples to upload: " + triplesToUpload.size());
        protocol.exec(triplesToUpload);
        return null;
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
                        trapdoors[j][permutation.get(i)] = protocol.generateTrapdoorAndIncrementIV(keyword);
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

    private Collection<Binding> decryptBindings(Collection<SerializableBinding> bindings, Map<Var, Var> obfuscationMap, Protocol1 protocol) throws
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
