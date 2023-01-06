package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ResultSetStream;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingBuilder;
import org.apache.jena.sparql.modify.request.QuadDataAcc;
import org.apache.jena.sparql.modify.request.UpdateDataInsert;
import org.apache.jena.update.Update;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.json.JSONObject;
import pt.fct.nova.id.srv.application.SPARQLQueryEngine;
import pt.fct.nova.id.srv.application.clients.*;
import pt.fct.nova.id.srv.application.protocols.Utils;
import pt.fct.nova.id.srv.application.protocols.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.protocols.Protocol1;
import pt.fct.nova.id.srv.application.query.QueryType;
import pt.fct.nova.id.srv.application.query.execution.SPARQLResult;
import pt.fct.nova.id.srv.application.query.jobs.*;
import pt.fct.nova.id.srv.application.query.plans.DefaultQueryExecutionPlan;
import pt.fct.nova.id.srv.application.query.plans.SecureSPARQLPlanner;
import pt.fct.nova.id.srv.presentation.api.EncryptedTriplestoreAPI;
import pt.fct.nova.id.srv.presentation.api.dtos.QueryForm;
import pt.fct.nova.id.srv.presentation.api.dtos.UploadForm;
import pt.fct.nova.id.srv.presentation.exceptions.UnknownRDFLanguageException;
import redis.clients.jedis.search.querybuilder.QueryBuilders;

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
import static pt.fct.nova.id.srv.application.protocols.EncryptionProtocol.COMPOUND_KEYWORD;
import static pt.fct.nova.id.srv.application.query.QueryType.*;
import static pt.fct.nova.id.srv.application.query.jobs.VariablesPattern.*;
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

    private Response answerSPARQLQuery(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID, String query, Protocol1 protocol, String accessToken) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, IOException, URISyntaxException, ClassNotFoundException {
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
                int total = Utils.integerFromByteArray(protocol.decryptRNDLayer(info));
                keywordsFrequencyCollector.put(keywords.get(i), total);
            } else
                keywordsFrequencyCollector.put(keywords.get(i), 0);
        }
        return null;
    }

    private Response executeSPARQLQuery(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID, QueryType queryType, SecureSPARQLPlanner planner, DefaultQueryExecutionPlan plan, Map<String, Integer> keywordsFrequency, Protocol1 protocol, String accessToken) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, IOException, URISyntaxException, InvalidKeyException, ClassNotFoundException {
        if (keywordsFrequency.containsValue(0)) {
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return getEmptySPARQLQueryResult(plan, planner.getObfuscationMap());
        }
        HTTPResponse response = prepareSearches(httpClient, protocol, triplestoreID, planner.getSearchJobsIDs(), plan.getJobs(),
                keywordsFrequency, new HashMap<>(), new HashMap<>(), accessToken);
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


    private Response executeSPARQLUpdateQuery(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID, QueryType queryType, SecureSPARQLPlanner planner, DefaultQueryExecutionPlan plan, Map<String, Integer> keywordsFrequency, Protocol1 protocol, String accessToken) throws IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, URISyntaxException, InvalidKeyException, ClassNotFoundException, InvalidNodeException {
        HTTPResponse response = acquireTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
        if (response.getStatus() != OK) {
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return response.build();
        }
        Map<String, String> triplesToUpload = new HashMap<>();
        List<String> trapdoorsToDelete = new LinkedList<>();
        if (queryType == DELETE_WHERE) {
            Map<String, String[][]> trapdoors = new HashMap<>();
            Map<String, List<String>> searchIDs = new HashMap<>();
            response = prepareSearches(httpClient, protocol, triplestoreID, planner.getSearchJobsIDs(), plan.getJobs(), keywordsFrequency, trapdoors, searchIDs, accessToken);
            if (response != null && response.getStatus() != OK) {
                releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }
            Map<String, List<String>> values = planner.getDeleteTemplate();
            //TODO: finish client logic for uploads & deletions + planner. create uri + finish proxy controller.
            Set<Integer> idxs;
            List<String> keywordSearchIDs;
            String[][] keywordTrapdoors;
            for (String keyword : trapdoors.keySet()) {
                keywordSearchIDs = searchIDs.get(keyword);
                keywordTrapdoors = trapdoors.get(keyword);
                for (int i = 0; i < keywordSearchIDs.size(); i++) {
                    response = testValues(httpClient, keywordSearchIDs.get(i), values.get(keyword), accessToken);
                    if (response.getStatus() != OK) {
                        releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                        deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                        return response.build();
                    }
                    idxs = ParsingUtils.parseSetOfIntegers(response.getBody());
                    if (!idxs.isEmpty()) {
                        for (Integer idx : idxs)
                            trapdoorsToDelete.add(keywordTrapdoors[i][idx]);
                    }
                }
            }
        }
        //return updateTriplestore(httpClient, cookie, triplestoreID, accessToken, triplesToUpload, trapdoorsToDelete);
        return Response.ok(NOT_IMPLEMENTED_ERROR).status(NOT_IMPLEMENTED).build();
    }


    private Response updateTriplestore(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID, String accessToken, Map<String, String> uploads, List<String> deletions) throws IOException, InvalidNodeException {
        JSONObject responseBody = new JSONObject();
        if (!deletions.isEmpty())
            responseBody = responseBody.put("Delete Response:", deleteSomeContents(httpClient, triplestoreID, deletions, accessToken).getBody());
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
            Graph g = pt.fct.nova.id.srv.application.query.Utils.generateGraphFromBindings(constructTemplate, bindings);
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
                                         Map<String, Integer> keywordsFrequency,
                                         Map<String, String[][]> trapdoorCollector, Map<String, List<String>> searchIDCollector, String accessToken) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, IOException, URISyntaxException {
        HTTPResponse response;
        String keyword;
        SecureSearchJob secureSearchJob;
        String[][] trapdoors;
        Var[] vars;
        List<Integer> shuffledIdxs;
        List<String> searchIDs;
        int keywordFrequency;
        for (String jobID : jobIDs) {
            secureSearchJob = (SecureSearchJob) jobs.get(jobID);
            vars = secureSearchJob.getVars();
            keyword = secureSearchJob.getSearches().get(vars[0]);
            searchIDs = searchIDCollector.get(keyword);
            if (searchIDs == null) {
                searchIDs = new ArrayList<>(vars.length);
                keywordFrequency = keywordsFrequency.get(keyword);
                shuffledIdxs = shuffledIdxs(keywordFrequency / vars.length);
                trapdoors = new String[vars.length][keywordFrequency / vars.length];
                for (int i = 0; i < keywordFrequency / vars.length; i++) {
                    for (int j = 0; j < vars.length; j++)
                        trapdoors[j][shuffledIdxs.get(i)] = protocol.generateTrapdoor(keyword);
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
                trapdoorCollector.put(keyword, trapdoors);
            } else {
                for (int i = 0; i < vars.length; i++) {
                    secureSearchJob.prepareSearch(vars[i], searchIDs.get(i));
                    System.out.println("[ " + vars[i] + " ] - " + " | " + searchIDs.get(i) + " | " + keyword);
                }
            }
        }
        return null;
    }

    private List<Integer> shuffledIdxs(int total) {
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
