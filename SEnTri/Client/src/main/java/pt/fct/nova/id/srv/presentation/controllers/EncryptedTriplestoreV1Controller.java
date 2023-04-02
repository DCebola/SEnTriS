package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingBuilder;
import org.apache.jena.sparql.modify.request.QuadDataAcc;
import org.apache.jena.sparql.modify.request.UpdateDataInsert;
import org.apache.jena.update.UpdateRequest;
import pt.fct.nova.id.srv.application.ontologies.DefaultOntology;
import pt.fct.nova.id.srv.application.ontologies.Ontology;
import pt.fct.nova.id.srv.application.ontologies.SecureOntology;
import pt.fct.nova.id.srv.application.query.SPARQLQueryEngine;
import pt.fct.nova.id.srv.application.clients.*;
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
import pt.fct.nova.id.srv.presentation.api.dtos.SchemaForm;
import pt.fct.nova.id.srv.presentation.api.dtos.TriplestoreForm;
import pt.fct.nova.id.srv.presentation.api.dtos.UploadForm;
import pt.fct.nova.id.srv.presentation.exceptions.UnknownRDFLanguageException;

import javax.crypto.AEADBadTagException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.*;

import static jakarta.ws.rs.core.Response.Status.*;
import static pt.fct.nova.id.srv.application.query.QueryType.*;
import static pt.fct.nova.id.srv.application.query.QueryUtils.generateID;
import static pt.fct.nova.id.srv.presentation.controllers.ParsingUtils.*;
import static pt.fct.nova.id.srv.presentation.controllers.TriplestoreController.*;
import static pt.fct.nova.id.srv.presentation.controllers.TriplestoreController.INTERNAL_ERROR;

@Path("triplestores/encrypted/v1")
public class EncryptedTriplestoreV1Controller extends EncryptedTriplestoreController implements EncryptedTriplestoreAPI {
    private static final SecureRandom rnd = new SecureRandom();
    private final String protocolVersion = "v1";

    @Override
    public Response create(Cookie cookie, TriplestoreForm form) {
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
            response = acquireTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
            if (response.getStatus() != OK) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }
            response = saveProtocolSecrets(httpClient, triplestoreID, generateSecretsMap(new Protocol1()), accessToken);
            if (response.getStatus() != OK) {
                HTTPResponse response2 = deleteTriplestoreAccessPolicy(httpClient, cookie, triplestoreID, accessToken);
                if (response2.getStatus() != OK)
                    return response2.build();
                releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }

            releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return Response.ok(SUCCESSFUL_CREATION).build();
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
            HTTPResponse response = deleteEncryptedTriplestore(httpClient, cookie, protocolVersion, triplestoreID, issuer);
            if (response.getStatus() != OK)
                return response.build();
            return Response.ok(SUCCESSFUL_DELETION).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response upload(Cookie cookie, boolean schema, UploadForm form) {
        if (cookie == null)
            return Response.ok(INVALID_COOKIE).status(BAD_REQUEST).build();
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            String issuer = form.getIssuer();
            String triplestoreID = form.getTriplestoreID();
            if (form.getContents() == null) {
                return Response.ok(EMPTY_UPLOAD).status(Response.Status.BAD_REQUEST).build();
            }
            List<Triple> triples = parseTriples(form.getContents(), parseRDFLanguage(form.getSyntax()));
            if (triples.isEmpty()) {
                return Response.ok(EMPTY_UPLOAD).status(Response.Status.BAD_REQUEST).build();
            }
            HTTPResponse response = createAccessToken(httpClient, cookie, issuer, triplestoreID);
            if (response.getStatus() != OK)
                return response.build();
            String accessToken = response.getBody();
            response = acquireTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
            if (response.getStatus() != OK) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }
            response = getProtocolSecrets(httpClient, triplestoreID, accessToken);
            if (response.getStatus() != OK) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }
            Map<String, String> secrets = ParsingUtils.parseMapOfStringString(response.getBody());
            Protocol1 protocol = getProtocol1(secrets);

            if (schema)
                return uploadOntologySchema(httpClient, cookie, triplestoreID, protocol, triples, accessToken);
            else {
                QuadDataAcc qc = new QuadDataAcc();
                triples.forEach(qc::addTriple);
                SecureSPARQLPlanner planner = new SecureSPARQLPlanner();
                DefaultQueryExecutionPlan plan = (DefaultQueryExecutionPlan) new SPARQLQueryEngine(planner).getQueryPlan(
                        new UpdateRequest().add(new UpdateDataInsert(qc)).toString());
                QueryType queryType = planner.getQueryType();
                return answerSPARQLQuery(httpClient, cookie, triplestoreID, queryType, planner, plan, protocol, accessToken);
            }
        } catch (UnknownRDFLanguageException e) {
            return Response.ok(INVALID_SYNTAX).status(Response.Status.BAD_REQUEST).build();
        } catch (InvalidNodeException e) {
            return Response.ok(BAD_NODE).status(Response.Status.BAD_REQUEST).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Response uploadOntologySchema(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID, Protocol1 protocol, List<Triple> triples, String accessToken) throws IOException, InvalidNodeException, AEADBadTagException {
        String schemaKeyword = protocol.getSchemaKeyword();
        int numTrapdoors = rnd.nextInt(minimumTrapdoors, maximumTrapdoors);
        List<String> trapdoors = new ArrayList<>(numTrapdoors);
        int rndIndex = rnd.nextInt(0, numTrapdoors - 1);
        for (int i = 0; i < numTrapdoors; i++) {
            if (i == rndIndex)
                trapdoors.add(protocol.generateKeywordsFrequencyTrapdoor(schemaKeyword));
            else
                trapdoors.add(protocol.generateKeywordsFrequencyTrapdoor(generateID()));
        }
        HTTPResponse response = searchEncryptedTriplestoreContents(httpClient, protocolVersion, triplestoreID, trapdoors, accessToken);
        if (response.getStatus() != OK) {
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
            return response.build();
        }
        String encryptedFrequency = ParsingUtils.parseListOfStrings(response.getBody()).get(rndIndex);
        int schemaFrequency = 0;
        if (encryptedFrequency != null)
            schemaFrequency = ParsingUtils.byteArrayToInteger(protocol.decryptRNDLayer(encryptedFrequency));
        System.out.println("SCHEMA FREQUENCY: " + schemaFrequency);
        if (schemaFrequency > 0) {
            Set<String> schemaTrapdoors = new HashSet<>();

            for (int i = 0; i < schemaFrequency; i++)
                schemaTrapdoors.add(protocol.generateTrapdoorAndIncrementIV(schemaKeyword));

            response = deleteSomeContents(httpClient, protocolVersion, triplestoreID, schemaTrapdoors, accessToken);
            if (response.getStatus() != OK) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }
        }
        protocol.exec(triples, true);
        response = upload(httpClient, protocolVersion, triplestoreID, protocol.getEncryptedNodes(), accessToken);
        deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
        releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
        return response.build();
    }

    @Override
    public Response fetchSchema(Cookie cookie, boolean inference, SchemaForm form) {
        if (cookie == null)
            return Response.ok(INVALID_COOKIE).status(BAD_REQUEST).build();
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            Lang lang = ParsingUtils.parseRDFLanguage(form.getSyntax());
            String triplestoreID = form.getTriplestoreID();
            HTTPResponse response = createAccessToken(httpClient, cookie, form.getIssuer(), triplestoreID);
            if (response.getStatus() != OK)
                return response.build();
            String accessToken = response.getBody();
            response = getProtocolSecrets(httpClient, triplestoreID, accessToken);
            if (response.getStatus() != OK) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }
            Map<String, String> secrets = ParsingUtils.parseMapOfStringString(response.getBody());
            Ontology ontology = new DefaultOntology(triplestoreID);
            response = fetchOntologySchema(httpClient, triplestoreID, getProtocol1(secrets), ontology, inference, accessToken);
            if (response != null && response.getStatus() != OK) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            RDFDataMgr.write(out, ontology.getModel(), lang);
            return Response.ok(out.toByteArray()).build();
        } catch (UnknownRDFLanguageException e) {
            return Response.ok(INVALID_SYNTAX).status(BAD_REQUEST).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    private HTTPResponse fetchOntologySchema(CloseableHttpClient httpClient, String triplestoreID, Protocol1 protocol, Ontology ontology, boolean inference, String accessToken) throws IOException, AEADBadTagException {
        String schemaKeyword = protocol.getSchemaKeyword();
        int numTrapdoors = rnd.nextInt(minimumTrapdoors, maximumTrapdoors);
        List<String> trapdoors = new ArrayList<>(numTrapdoors);
        int rndIndex = rnd.nextInt(0, numTrapdoors - 1);
        for (int i = 0; i < numTrapdoors; i++) {
            if (i == rndIndex)
                trapdoors.add(protocol.generateKeywordsFrequencyTrapdoor(schemaKeyword));
            else
                trapdoors.add(protocol.generateKeywordsFrequencyTrapdoor(generateID()));
        }
        HTTPResponse response = searchEncryptedTriplestoreContents(httpClient, protocolVersion, triplestoreID, trapdoors, accessToken);
        if (response.getStatus() != OK)
            return response;

        String encryptedFrequency = ParsingUtils.parseListOfStrings(response.getBody()).get(rndIndex);
        int schemaFrequency = 0;
        if (encryptedFrequency != null)
            schemaFrequency = ParsingUtils.byteArrayToInteger(protocol.decryptRNDLayer(encryptedFrequency));
        if (schemaFrequency > 0) {
            System.out.println("SCHEMA FREQUENCY: " + schemaFrequency);
            List<String> schemaTrapdoors = new ArrayList<>(schemaFrequency);

            for (int i = 0; i < schemaFrequency; i++)
                schemaTrapdoors.add(protocol.generateTrapdoorAndIncrementIV(schemaKeyword));

            response = searchEncryptedTriplestoreContents(httpClient, protocolVersion, triplestoreID, schemaTrapdoors, accessToken);
            if (response.getStatus() != OK)
                return response;

            List<String> encryptedSchema = ParsingUtils.parseListOfStrings(response.getBody());
            Set<Triple> schema = new HashSet<>();
            for (int i = 0; i < schemaFrequency; i += 3) {
                schema.add(Triple.create(
                        generateNode(new String(protocol.decryptRNDLayer(encryptedSchema.get(i)))),
                        generateNode(new String(protocol.decryptRNDLayer(encryptedSchema.get(i + 1)))),
                        generateNode(new String(protocol.decryptRNDLayer(encryptedSchema.get(i + 2))))
                ));
            }
            System.out.println("SCHEMA: " + schema.size());
            ontology.execInference(schema, inference);
        }
        return null;
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
            Protocol1 protocol = getProtocol1(secrets);
            SecureSPARQLPlanner planner;
            System.out.println("INFERENCE: " + form.getInference());
            if (form.getInference()) {
                Ontology ontology = new SecureOntology(triplestoreID, form.getTransitivityDepth(), form.getExpansionDepth());
                response = fetchOntologySchema(httpClient, triplestoreID, protocol, ontology, form.getInference(), accessToken);
                if (response != null && response.getStatus() != OK) {
                    deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                    return response.build();
                }
                planner = new SecureSPARQLPlanner(ontology);
            } else
                planner = new SecureSPARQLPlanner();
            DefaultQueryExecutionPlan plan = (DefaultQueryExecutionPlan) new SPARQLQueryEngine(planner).getQueryPlan(form.getQuery());
            QueryType queryType = planner.getQueryType();

            boolean isUpdate = queryType == INSERT_DATA || queryType == DELETE_DATA || queryType == DELETE_WHERE || queryType == MODIFY;
            if (isUpdate) {
                response = acquireTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                if (response.getStatus() != OK) {
                    deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                    return response.build();
                }
            }
            return answerSPARQLQuery(httpClient, cookie, triplestoreID, queryType, planner, plan, protocol, accessToken);
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    private Response answerSPARQLQuery(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID, QueryType queryType, SecureSPARQLPlanner planner,
                                       DefaultQueryExecutionPlan plan, Protocol1 protocol, String accessToken) throws InvalidNodeException, IOException, URISyntaxException, ClassNotFoundException, AEADBadTagException {
        System.out.println("QUERY TYPE: " + queryType);
        return switch (queryType) {
            case SELECT, ASK, DESCRIBE, CONSTRUCT ->
                    executeSPARQLQuery(httpClient, cookie, triplestoreID, queryType, planner, plan, protocol, accessToken);
            case INSERT_DATA, DELETE_DATA, DELETE_WHERE, MODIFY ->
                    executeSPARQLUpdateQuery(httpClient, cookie, triplestoreID, queryType, planner, plan, protocol, accessToken);
        };
    }

    private HTTPResponse fetchKeywordsFrequencies(HttpClient httpClient, String triplestoreID, List<String> keywords,
                                                  Map<String, Integer> keywordsFrequencyCollector, Protocol1 protocol, String accessToken) throws AEADBadTagException, IOException {
        List<String> trapdoors = new ArrayList<>(keywords.size());
        for (String keyword : keywords)
            trapdoors.add(protocol.generateKeywordsFrequencyTrapdoor(keyword));
        HTTPResponse response = searchEncryptedTriplestoreContents(httpClient, protocolVersion, triplestoreID, trapdoors, accessToken);
        if (response.getStatus() != OK)
            return response;
        List<String> encryptedKeywordsFrequencies = ParsingUtils.parseListOfStrings(response.getBody());
        String frequency;
        for (int i = 0; i < encryptedKeywordsFrequencies.size(); i++) {
            frequency = encryptedKeywordsFrequencies.get(i);
            if (frequency != null)
                keywordsFrequencyCollector.put(keywords.get(i), ParsingUtils.byteArrayToInteger(protocol.decryptRNDLayer(frequency)));
            else
                keywordsFrequencyCollector.put(keywords.get(i), 0);
        }
        return null;
    }

    private Response executeSPARQLQuery(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID,
                                        QueryType queryType, SecureSPARQLPlanner planner, DefaultQueryExecutionPlan plan,
                                        Protocol1 protocol, String accessToken) throws IOException, AEADBadTagException, URISyntaxException, ClassNotFoundException {
        Map<String, Integer> keywordsFrequency = new HashMap<>();
        List<String> keywords = planner.getKeywords().stream().toList();

        HTTPResponse response = fetchKeywordsFrequencies(httpClient, triplestoreID, keywords, keywordsFrequency, protocol, accessToken);
        if (response != null && response.getStatus() != OK) {
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return response.build();
        }
        response = prepareSearches(httpClient, protocol, triplestoreID, planner.getSearchJobsIDs(), plan.getJobs(), keywordsFrequency, accessToken);
        if (response != null && response.getStatus() != OK) {
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return response.build();
        }
        response = query(httpClient, protocolVersion, protocol.getRNDKey().getEncoded(), plan, accessToken);
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
                                              DefaultQueryExecutionPlan plan, Protocol1 protocol, String accessToken) throws IOException, URISyntaxException, ClassNotFoundException, InvalidNodeException, AEADBadTagException {
        Map<String, String> swaps = new HashMap<>();
        Set<String> deletions = new HashSet<>();
        List<Triple> triplesToUpload, triplesToDelete;
        Map<String, Integer> keywordsFrequency = new HashMap<>();
        if (queryType == DELETE_WHERE || queryType == MODIFY) {
            HTTPResponse response = fetchKeywordsFrequencies(httpClient, triplestoreID, planner.getKeywords().stream().toList(), keywordsFrequency, protocol, accessToken);
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
            System.out.println(Arrays.toString(plan.getVars().toArray()));
            response = query(httpClient, protocolVersion, protocol.getRNDKey().getEncoded(), plan, accessToken);
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
            triplesToUpload = new ArrayList<>(0);
        }

        System.out.println("Triples to Upload: " + triplesToUpload.size());
        System.out.println("Triples to Delete: " + triplesToDelete.size());
        HTTPResponse response = computeDeletionsSwapsAndUploads(httpClient, triplestoreID, protocol, keywordsFrequency,
                triplesToDelete, triplesToUpload, swaps, deletions, accessToken);
        if (response != null && response.getStatus() != OK) {
            releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return response.build();
        }
        return updateTriplestore(httpClient, cookie, protocolVersion, triplestoreID, protocol.getEncryptedNodes(), deletions, swaps, accessToken);
    }

    private HTTPResponse computeDeletionsSwapsAndUploads(CloseableHttpClient httpClient, String triplestoreID, Protocol1 protocol,
                                                         Map<String, Integer> keywordsFrequency, List<Triple> triplesToDelete, List<Triple> triplesToUpload,
                                                         Map<String, String> swapsCollector, Set<String> deletionsCollector,
                                                         String accessToken) throws IOException, InvalidNodeException, AEADBadTagException {
        Set<String> keywords = ParsingUtils.generateKeywords(triplesToDelete);
        keywords.removeAll(keywordsFrequency.keySet());
        HTTPResponse response;
        if (!keywords.isEmpty()) {
            response = fetchKeywordsFrequencies(httpClient, triplestoreID, keywords.stream().toList(), keywordsFrequency, protocol, accessToken);
            if (response != null && response.getStatus() != OK)
                return response;
        }
        protocol.setKeywordFrequencies(keywordsFrequency);
        Map<String, List<String>> keywordsTrapdoors = protocol.generateKeywordsPatternTrapdoors(triplesToDelete);
        List<String> keywordList = new ArrayList<>(keywordsTrapdoors.keySet());

        Map<String, Set<Integer>> keywordsInstances = new HashMap<>(keywordList.size());
        List<String> encryptedInstances;
        Set<Integer> instancesToDelete;
        String encryptedInstance;

        List<String> trapdoors = new ArrayList<>(keywordsTrapdoors.values().stream().mapToInt(List::size).sum());
        for (String keyword : keywordList)
            trapdoors.addAll(keywordsTrapdoors.get(keyword));

        System.out.println(trapdoors.size());
        response = searchEncryptedTriplestoreContents(httpClient, protocolVersion, triplestoreID, trapdoors, accessToken);
        if (response.getStatus() != OK)
            return response;
        encryptedInstances = ParsingUtils.parseListOfStrings(response.getBody());

        int offset = 0;
        int length;
        for (String keyword : keywordList) {
            length = keywordsTrapdoors.get(keyword).size();
            instancesToDelete = new HashSet<>();
            for (int i = offset; i < offset + length; i++) {
                encryptedInstance = encryptedInstances.get(i);
                if (encryptedInstance != null) {
                    instancesToDelete.add(ParsingUtils.byteArrayToInteger(protocol.decryptRNDLayer(encryptedInstance)));
                    deletionsCollector.add(trapdoors.get(i));
                }
            }
            if (!instancesToDelete.isEmpty())
                keywordsInstances.put(keyword, instancesToDelete);

            offset += length;
        }

        System.out.println("Keyword Instances to Delete: " + keywordsInstances.size());

        int frequency, swaps, deletions, preserved, totalPreserved = 0;
        Queue<Integer> instancesToKeep;
        for (String keyword : keywordsInstances.keySet()) {
            swaps = 0;
            deletions = 0;
            instancesToDelete = keywordsInstances.get(keyword);
            frequency = keywordsFrequency.get(keyword);
            instancesToKeep = new ArrayDeque<>(frequency - instancesToDelete.size());

            for (int i = frequency; i > 0; i--) {
                if (!instancesToDelete.contains(i))
                    instancesToKeep.add(i);
            }

            if (!instancesToKeep.isEmpty()) {
                System.out.println("KEYWORD: " + keyword + " | " + frequency);
                System.out.println("TO DELETE:" + Arrays.toString(instancesToDelete.toArray()));
                System.out.println("EXPECTED:" + Arrays.toString(instancesToKeep.toArray()));
                Integer cur;
                for (int i = 1; i <= frequency; i++) {
                    if (instancesToDelete.contains(i)) {
                        cur = instancesToKeep.peek();
                        if (cur != null && cur > i) {
                            swaps += 1;
                            instancesToDelete.remove(i);
                            System.out.print(cur + " -> " + i + " | ");
                            swapsCollector.put(protocol.generateTrapdoor(keyword, cur), protocol.generateTrapdoor(keyword, i));
                            instancesToKeep.poll();
                            protocol.deleteKeyword(keyword);
                        }
                    }
                }
                System.out.println();
            }


            for (int i : instancesToDelete) {
                deletionsCollector.add(protocol.generateTrapdoor(keyword, i));
                protocol.deleteKeyword(keyword);
                deletions += 1;
            }

            preserved = frequency - deletions - swaps;
            totalPreserved += preserved;
            if (!instancesToKeep.isEmpty()) {
                System.out.println("Deleted: " + Arrays.toString(instancesToDelete.toArray()));
                System.out.println("[ " + keyword + "] - f" + protocol.getKeywordFrequencies().get(keyword) + " | p" + preserved + " | d" + deletions + " | s" + swaps);
            }
        }
        protocol.exec(triplesToUpload, false);
        System.out.println("PRESERVED:" + totalPreserved);
        System.out.println("KEYWORD FREQUENCIES TRAPDOORS:" + protocol.getKeywordFrequencies().size());
        return null;
    }

    private HTTPResponse prepareSearches(CloseableHttpClient httpClient, Protocol1 protocol,
                                         String triplestoreID, Set<String> jobIDs, Map<String, Job> jobs,
                                         Map<String, Integer> keywordsFrequency, String accessToken) throws IOException, URISyntaxException {
        HTTPResponse response;
        String keyword;
        SecureSearchJob secureSearchJob;
        String[][] trapdoors;
        Var[] vars;
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
                trapdoors = new String[vars.length][keywordFrequency / vars.length];
                for (int i = 0; i < keywordFrequency / vars.length; i++) {
                    for (int j = 0; j < vars.length; j++)
                        trapdoors[j][i] = protocol.generateTrapdoorAndIncrementIV(keyword);
                }
                for (int i = 0; i < vars.length; i++) {
                    response = prepareSearch(httpClient, protocolVersion, triplestoreID, List.of(trapdoors[i]), accessToken);
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

    private Collection<Binding> decryptBindings(Collection<SerializableBinding> bindings, Map<Var, Var> obfuscationMap, Protocol1 protocol) throws AEADBadTagException {
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
