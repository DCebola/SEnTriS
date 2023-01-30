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

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
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
    public static final String SECRETS_KEY = System.getenv("SECRETS_PROTOCOL_KEY");
    public static final String SECRETS_IV = System.getenv("SECRETS_PROTOCOL_IV");
    public static final String SECRETS_SCHEMA_KEYWORD = System.getenv("SECRETS_PROTOCOL_SCHEMA_KEYWORD");
    public static final String SUCCESSFUL_CREATION = "Successful creation.";
    public static final String EMPTY_UPLOAD = "No content to upload.";
    private static final String BAD_NODE = "Data must only contain concrete nodes: IRI, Blank, Literal.";
    private static final String NO_UPDATES = "No content to update.";
    private static final int minimumTrapdoors = Integer.parseInt(System.getenv("MINIMUM_TRAPDOORS_PER_SEARCH"));
    private static final int maximumTrapdoors = Integer.parseInt(System.getenv("MAXIMUM_TRAPDOORS_PER_SEARCH"));
    private static final SecureRandom rnd = new SecureRandom();


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
            HTTPResponse response = deleteEncryptedTriplestore(httpClient, cookie, triplestoreID, issuer);
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
                Collections.shuffle(triples);
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

    private Response uploadOntologySchema(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID, Protocol1 protocol, List<Triple> triples, String accessToken) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, IOException, InvalidNodeException {
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
        HTTPResponse response = searchEncryptedTriplestoreContents(httpClient, triplestoreID, trapdoors, accessToken);
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
            String[] schemaTrapdoors = new String[schemaFrequency];
            List<Integer> permutation = generateRandomPermutation(schemaFrequency);

            for (int i = 0; i < schemaFrequency; i++)
                schemaTrapdoors[permutation.get(i)] = protocol.generateTrapdoorAndIncrementIV(schemaKeyword);

            response = deleteSomeContents(httpClient, triplestoreID, Set.of(schemaTrapdoors), accessToken);
            if (response.getStatus() != OK) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }
        }
        Collections.shuffle(triples);
        protocol.exec(triples, true);
        response = upload(httpClient, triplestoreID, protocol.getEncryptedNodes(), accessToken);
        if (response.getStatus() != OK) {
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
        }
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

    private HTTPResponse fetchOntologySchema(CloseableHttpClient httpClient, String triplestoreID, Protocol1 protocol, Ontology ontology, boolean inference, String accessToken) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, IOException {
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
        HTTPResponse response = searchEncryptedTriplestoreContents(httpClient, triplestoreID, trapdoors, accessToken);
        if (response.getStatus() != OK)
            return response;

        String encryptedFrequency = ParsingUtils.parseListOfStrings(response.getBody()).get(rndIndex);
        int schemaFrequency = 0;
        if (encryptedFrequency != null)
            schemaFrequency = ParsingUtils.byteArrayToInteger(protocol.decryptRNDLayer(encryptedFrequency));
        if (schemaFrequency > 0) {
            System.out.println("SCHEMA FREQUENCY: " + schemaFrequency);
            String[] schemaTrapdoors = new String[schemaFrequency];
            List<Integer> permutation = generateRandomPermutation(schemaFrequency);

            for (int i = 0; i < schemaFrequency; i++)
                schemaTrapdoors[permutation.get(i)] = protocol.generateTrapdoorAndIncrementIV(schemaKeyword);

            response = searchEncryptedTriplestoreContents(httpClient, triplestoreID, List.of(schemaTrapdoors), accessToken);
            if (response.getStatus() != OK)
                return response;

            List<String> encryptedSchema = ParsingUtils.parseListOfStrings(response.getBody());
            Set<Triple> schema = new HashSet<>();
            for (int i = 0; i < schemaFrequency; i += 3) {
                schema.add(Triple.create(
                        generateNode(new String(protocol.decryptRNDLayer(encryptedSchema.get(permutation.get(i))))),
                        generateNode(new String(protocol.decryptRNDLayer(encryptedSchema.get(permutation.get(i + 1))))),
                        generateNode(new String(protocol.decryptRNDLayer(encryptedSchema.get(permutation.get(i + 2)))))
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
            if (form.getInference()) {
                Ontology ontology = new DefaultOntology(triplestoreID, form.getTransitivityDepth(), form.getExpansionDepth());
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
                                       DefaultQueryExecutionPlan plan, Protocol1 protocol, String accessToken) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, IOException, URISyntaxException, ClassNotFoundException, InvalidNodeException {
        System.out.println("QUERY TYPE: " + queryType);
        return switch (queryType) {
            case SELECT, ASK, DESCRIBE, CONSTRUCT ->
                    executeSPARQLQuery(httpClient, cookie, triplestoreID, queryType, planner, plan, protocol, accessToken);
            case INSERT_DATA, DELETE_DATA, DELETE_WHERE, MODIFY ->
                    executeSPARQLUpdateQuery(httpClient, cookie, triplestoreID, queryType, planner, plan, protocol, accessToken);
        };
    }

    private HTTPResponse fetchKeywordsFrequencies(HttpClient httpClient, String triplestoreID, Set<String> keywords, Map<String, Integer> keywordsFrequencyCollector, Protocol1 protocol, String accessToken) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, IOException {
        String[] shuffledKeywords = new String[keywords.size()];
        String[] trapdoors = new String[keywords.size()];
        List<Integer> permutation = generateRandomPermutation(keywords.size());
        int i = 0;
        for (String keyword : keywords) {
            shuffledKeywords[permutation.get(i)] = keyword;
            trapdoors[permutation.get(i)] = protocol.generateKeywordsFrequencyTrapdoor(keyword);
            i++;
        }
        HTTPResponse response = searchEncryptedTriplestoreContents(httpClient, triplestoreID, List.of(trapdoors), accessToken);
        if (response.getStatus() != OK)
            return response;
        List<String> encryptedKeywordsFrequencies = ParsingUtils.parseListOfStrings(response.getBody());
        String frequency;
        for (i = 0; i < encryptedKeywordsFrequencies.size(); i++) {
            frequency = encryptedKeywordsFrequencies.get(i);
            if (frequency != null)
                keywordsFrequencyCollector.put(shuffledKeywords[permutation.get(i)], ParsingUtils.byteArrayToInteger(protocol.decryptRNDLayer(frequency)));
            else
                keywordsFrequencyCollector.put(shuffledKeywords[permutation.get(i)], 0);
        }
        return null;
    }

    private Response executeSPARQLQuery(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID,
                                        QueryType queryType, SecureSPARQLPlanner planner, DefaultQueryExecutionPlan plan,
                                        Protocol1 protocol, String accessToken) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, IOException, URISyntaxException, InvalidKeyException, ClassNotFoundException {
        Map<String, Integer> keywordsFrequency = new HashMap<>();
        Set<String> keywords = planner.getKeywords();

        HTTPResponse response = fetchKeywordsFrequencies(httpClient, triplestoreID, keywords, keywordsFrequency, protocol, accessToken);
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
        Map<String, String> swaps = new HashMap<>();
        Set<String> deletions = new HashSet<>();
        List<Triple> triplesToUpload, triplesToDelete;
        Map<String, Integer> keywordsFrequency = new HashMap<>();
        if (queryType == DELETE_WHERE || queryType == MODIFY) {
            HTTPResponse response = fetchKeywordsFrequencies(httpClient, triplestoreID, planner.getKeywords(), keywordsFrequency, protocol, accessToken);
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
            triplesToUpload = new ArrayList<>(0);
        }
        Collections.shuffle(triplesToDelete);
        Collections.shuffle(triplesToUpload);
        System.out.println("Triples to Upload: " + triplesToUpload.size());
        System.out.println("Triples to Delete: " + triplesToDelete.size());
        HTTPResponse response = computeDeletionsSwapsAndUploads(httpClient, triplestoreID, protocol, keywordsFrequency,
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
        Set<String> keywords = new HashSet<>(ParsingUtils.generateKeywords(triplesToDelete));
        keywords.removeAll(keywordsFrequency.keySet());
        HTTPResponse response;
        if (!keywords.isEmpty()) {
            response = fetchKeywordsFrequencies(httpClient, triplestoreID, keywords, keywordsFrequency, protocol, accessToken);
            if (response != null && response.getStatus() != OK)
                return response;
        }
        protocol.setKeywordFrequencies(keywordsFrequency);
        Map<String, List<String>> keywordsTrapdoors = protocol.generateKeywordsPatternTrapdoors(triplesToDelete);
        List<String> keywordList = new ArrayList<>(keywordsTrapdoors.keySet());

        Map<String, Set<Integer>> keywordsInstances = new HashMap<>(keywordList.size());
        List<String> encryptedInstances, trapdoors;
        Set<Integer> instancesToDelete;
        String encryptedInstance;
        int totalTrapdoors = keywordsTrapdoors.values().stream().mapToInt(List::size).sum();
        String[] trapdoorsToDelete = new String[totalTrapdoors];
        List<Integer> permutation = generateRandomPermutation(totalTrapdoors);
        int offset = 0, length;
        for (String keyword : keywordList) {
            trapdoors = keywordsTrapdoors.get(keyword);
            length = trapdoors.size();
            for (int i = 0; i < length; i++)
                trapdoorsToDelete[permutation.get(offset + i)] = trapdoors.get(i);
            offset += length;
        }
        System.out.println(trapdoorsToDelete.length);
        response = searchEncryptedTriplestoreContents(httpClient, triplestoreID, List.of(trapdoorsToDelete), accessToken);
        if (response.getStatus() != OK)
            return response;
        encryptedInstances = ParsingUtils.parseListOfStrings(response.getBody());

        offset = 0;
        for (String keyword : keywordList) {
            length = keywordsTrapdoors.get(keyword).size();
            instancesToDelete = new HashSet<>();
            for (int i = offset; i < offset + length; i++) {
                encryptedInstance = encryptedInstances.get(permutation.get(i));
                if (encryptedInstance != null) {
                    instancesToDelete.add(ParsingUtils.byteArrayToInteger(protocol.decryptRNDLayer(encryptedInstance)));
                    deletionsCollector.add(trapdoorsToDelete[permutation.get(i)]);
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
