package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
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
import pt.fct.nova.id.srv.application.query.plans.QueryExecutionPlan;
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
import static pt.fct.nova.id.srv.application.query.QueryType.ASK;
import static pt.fct.nova.id.srv.application.query.QueryType.CONSTRUCT;
import static pt.fct.nova.id.srv.application.query.QueryType.DESCRIBE;
import static pt.fct.nova.id.srv.application.query.QueryType.SELECT;
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
        if(cookie == null)
            return Response.ok(INVALID_COOKIE).status(BAD_REQUEST).build();
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            String triplestoreID = form.getTriplestoreID();
            String issuer = form.getIssuer();
            //TODO: obfuscate triplestore name
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

            response = uploadEncryptedTriplestoreContents(httpClient, triplestoreID, p.getEncryptedT(), accessToken);
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
        if(cookie == null)
            return Response.ok(INVALID_COOKIE).status(BAD_REQUEST).build();
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            HTTPResponse response = EncryptedTriplestoreController.deleteEncryptedTriplestore(httpClient, cookie, triplestoreID, issuer);
            if (response.getStatus() != OK)
                return response.build();
            return Response.ok(SUCCESSFUL_DELETION).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response upload(Cookie cookie, UploadForm form) {
        if(cookie == null)
            return Response.ok(INVALID_COOKIE).status(BAD_REQUEST).build();
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            String issuer = form.getIssuer();
            String triplestoreID = form.getTriplestoreID();

            HTTPResponse response = createAccessToken(httpClient, cookie, issuer, triplestoreID);
            if (response.getStatus() != OK)
                return response.build();
            String accessToken = response.getBody();

            response = acquireTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
            if (response.getStatus() != OK) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }

            if (form.getContents() == null)
                return Response.ok(EMPTY_UPLOAD).status(Response.Status.BAD_REQUEST).build();

            List<Triple> triples = parseTriples(form.getContents(), parseRDFLanguage(form.getSyntax()));
            if (triples.isEmpty())
                return Response.ok(EMPTY_UPLOAD).status(Response.Status.BAD_REQUEST).build();

            response = getProtocolSecrets(httpClient, triplestoreID, accessToken);
            if (response.getStatus() != OK) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }
            Map<String, String> secrets = ParsingUtils.parseMapOfStringString(response.getBody());

            Protocol1 protocol = getProtocol1(secrets);
            Collections.shuffle(triples);
            Map<String, String> trapdoors = protocol.init(triples);
            response = fetchKeywordsFrequencyAndUpdateProtocol(httpClient, triplestoreID, protocol, trapdoors, accessToken);
            if (response != null && response.getStatus() != OK) {
                releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            }

            Collections.shuffle(triples);
            protocol.exec(triples);
            response = uploadEncryptedTriplestoreContents(httpClient, triplestoreID, protocol.getEncryptedT(), accessToken);
            releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return response.build();
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
        if(cookie == null)
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

            Protocol1 protocol = getProtocol1(secrets);
            SecureSPARQLPlanner planner = new SecureSPARQLPlanner();
            DefaultQueryExecutionPlan plan = (DefaultQueryExecutionPlan) new SPARQLQueryEngine(planner).getQueryPlan(form.getQuery());

            Set<String> keywords = planner.getKeywords();

            List<String> keywordList = new ArrayList<>(keywords.size());
            List<String> trapdoors = new ArrayList<>(keywords.size());

            for (String keyword : keywords) {
                keywordList.add(keyword);
                trapdoors.add(protocol.generateKeywordsFrequencyTrapdoor(keyword));
            }

            response = searchEncryptedTriplestoreContents(httpClient, triplestoreID, trapdoors, accessToken);
            if (response.getStatus() != OK)
                return response.build();
            Map<Var, Var> obfuscationMap = planner.getObfuscationMap();
            List<String> encryptedKeywordsFrequencies = ParsingUtils.parseListOfStrings(response.getBody());
            Map<String, Integer> keywordsFrequency = new HashMap<>(encryptedKeywordsFrequencies.size());
            String info;
            for (int i = 0; i < encryptedKeywordsFrequencies.size(); i++) {
                info = encryptedKeywordsFrequencies.get(i);
                if (info != null) {
                    int total = Utils.integerFromByteArray(protocol.decryptRNDLayer(info));
                    keywordsFrequency.put(keywordList.get(i), total);
                } else
                    return getEmptySPARQLQueryResult(plan, obfuscationMap);
            }

            response = prepareSearches(httpClient, protocol, triplestoreID, plan, planner.getSearchJobsIDs(), keywordsFrequency, accessToken);
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


            QueryType queryType = planner.getQueryType();
            Response res;
            if (queryType == SELECT || queryType == CONSTRUCT) {
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
                res = generateDESCRIBEResults(plan.getVars(), obfuscationMap, sparqlResult);
            else
                res = Response.ok(NOT_IMPLEMENTED_ERROR).status(INTERNAL_SERVER_ERROR).build();
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
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
                                         String triplestoreID, QueryExecutionPlan plan, Set<String> searchJobsIDs,
                                         Map<String, Integer> keywordsFrequency, String accessToken) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, IOException, URISyntaxException {
        HTTPResponse response;
        String keyword;
        SecureSearchJob secureSearchJob;
        Map<String, Job> jobs = plan.getJobs();
        String[][] trapdoors;
        Var[] vars;
        List<Integer> shuffledIdxs;
        Map<String, List<String>> preparedKeywords = new HashMap<>();
        List<String> searchIDs;
        int keywordFrequency;
        for (String jobID : searchJobsIDs) {
            secureSearchJob = (SecureSearchJob) jobs.get(jobID);
            vars = secureSearchJob.getVars();
            keyword = secureSearchJob.getSearches().get(vars[0]);
            searchIDs = preparedKeywords.get(keyword);
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
                preparedKeywords.put(keyword, searchIDs);
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
