package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingBuilder;
import org.apache.jena.sparql.modify.request.QuadDataAcc;
import org.apache.jena.sparql.modify.request.UpdateDataInsert;
import org.apache.jena.update.UpdateRequest;
import pt.fct.nova.id.srv.application.clients.EncryptedTriplestoreV2Client;
import pt.fct.nova.id.srv.application.clients.HTTPClient;
import pt.fct.nova.id.srv.application.clients.HTTPResponse;
import pt.fct.nova.id.srv.application.crypto.dgk.DGKPrivateKey;
import pt.fct.nova.id.srv.application.crypto.dgk.DGKPublicKey;
import pt.fct.nova.id.srv.application.crypto.dgk.DGKUtils;
import pt.fct.nova.id.srv.application.ontologies.DefaultOntology;
import pt.fct.nova.id.srv.application.ontologies.Ontology;
import pt.fct.nova.id.srv.application.ontologies.SecureOntology;
import pt.fct.nova.id.srv.application.query.QueryType;
import pt.fct.nova.id.srv.application.query.QueryUtils;
import pt.fct.nova.id.srv.application.query.SPARQLQueryEngine;
import pt.fct.nova.id.srv.application.query.execution.SPARQLResult;
import pt.fct.nova.id.srv.application.query.jobs.Job;
import pt.fct.nova.id.srv.application.query.jobs.SecureSearchJob;
import pt.fct.nova.id.srv.application.query.jobs.SerializableBinding;
import pt.fct.nova.id.srv.application.query.plans.DefaultQueryExecutionPlan;
import pt.fct.nova.id.srv.application.query.plans.SecureSPARQLPlanner;
import pt.fct.nova.id.srv.application.schemes.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.schemes.EncryptionSchemeV2;
import pt.fct.nova.id.srv.presentation.api.EncryptedTriplestoreAPI;
import pt.fct.nova.id.srv.presentation.api.dtos.QueryForm;
import pt.fct.nova.id.srv.presentation.api.dtos.SchemaForm;
import pt.fct.nova.id.srv.presentation.api.dtos.TriplestoreForm;
import pt.fct.nova.id.srv.presentation.api.dtos.UploadForm;
import pt.fct.nova.id.srv.presentation.exceptions.UnknownRDFLanguageException;

import javax.crypto.AEADBadTagException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.*;

import static jakarta.ws.rs.core.Response.Status.*;
import static pt.fct.nova.id.srv.application.query.QueryType.*;
import static pt.fct.nova.id.srv.application.query.QueryUtils.generateID;
import static pt.fct.nova.id.srv.presentation.controllers.ParsingUtils.*;
import static pt.fct.nova.id.srv.presentation.controllers.TriplestoreController.INTERNAL_ERROR;
import static pt.fct.nova.id.srv.presentation.controllers.TriplestoreController.*;

@Path("triplestores/encrypted/v2")
public class EncryptedTriplestoreV2Controller extends EncryptedTriplestoreController implements EncryptedTriplestoreAPI {

    public static final String SECRETS_KEY_PAIR = System.getenv("SECRETS_PROTOCOL_KEY_PAIR");

    public static final String SECRETS_LAST_EQ_TAG = System.getenv("SECRETS_LAST_EQ_TAG");

    private static final SecureRandom rnd = new SecureRandom();
    private static final String protocolVersion = "v2";

    @Override
    public Response create(Cookie cookie, TriplestoreForm form) {
        if (cookie == null)
            return Response.ok(INVALID_COOKIE).status(BAD_REQUEST).build();
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            String triplestoreID = form.getTriplestoreID();
            String issuer = form.getIssuer();
            HTTPResponse response = createTriplestoreAccessList(httpClient, cookie, triplestoreID, issuer);
            if (response.getStatus() != OK)
                return response.build();

            response = createAccessToken(httpClient, cookie, issuer, triplestoreID);
            if (response.getStatus() != OK)
                return response.build();
            System.out.println("Got token");

            String accessToken = response.getBody();
            response = acquireTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
            if (response.getStatus() != OK) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }
            System.out.println("Acquired lock.");
            response = saveSecrets(httpClient, triplestoreID, generateSecretsMap(new EncryptionSchemeV2()), accessToken);
            if (response.getStatus() != OK) {
                System.out.println("Error saving secrets.");
                HTTPResponse response2 = deleteTriplestoreAccessList(httpClient, cookie, triplestoreID, accessToken);
                if (response2.getStatus() != OK) {
                    deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                    return response2.build();
                }
                return response.build();
            }

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
            if (form.getContent() == null) {
                return Response.ok(EMPTY_UPLOAD).status(Response.Status.BAD_REQUEST).build();
            }
            Set<Triple> triples = parseTriples(form.getContent(), parseRDFLanguage(form.getSyntax()));
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
            response = getSecrets(httpClient, triplestoreID, accessToken);
            if (response.getStatus() != OK) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }
            Map<String, String> secrets = ParsingUtils.parseSecretsMap(response.getBody());
            EncryptionSchemeV2 protocol = getProtocol2(secrets);
            if (schema) {
                LinkedList<Triple> l = new LinkedList<>(triples);
                triples.clear();
                return uploadOntologySchema(httpClient, cookie, triplestoreID, protocol, l, accessToken);
            } else {
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
            e.printStackTrace();
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Response uploadOntologySchema(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID, EncryptionSchemeV2 protocol,
                                          LinkedList<Triple> triples, String accessToken) throws IOException {
        try {
            String schemaKeyword = protocol.getSchemaKeyword();
            int numTrapdoors = rnd.nextInt(MIN_TRAPDOORS, MAX_TRAPDOORS);
            List<String> trapdoors = new ArrayList<>(numTrapdoors);
            int rndIndex = rnd.nextInt(0, numTrapdoors - 1);
            for (int i = 0; i < numTrapdoors; i++) {
                if (i == rndIndex)
                    trapdoors.add(protocol.generateTrapdoor(schemaKeyword));
                else
                    trapdoors.add(protocol.generateTrapdoor(generateID()));
            }
            HTTPResponse response = searchEncryptedTriplestoreContents(httpClient, protocolVersion, triplestoreID, trapdoors, accessToken);
            System.out.println("Searched contents.");
            if (response.getStatus() != OK) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }
            byte[] encryptedFrequency = ParsingUtils.parseListOfBytes(response.getBody()).get(rndIndex);
            int schemaFrequency = 0;
            if (encryptedFrequency != null)
                schemaFrequency = ParsingUtils.byteArrayToInteger(protocol.decryptRNDLayer(encryptedFrequency));

            System.out.println("SCHEMA FREQUENCY: " + schemaFrequency);
            List<String> deletions = new LinkedList<>();
            List<String> uploads = new LinkedList<>();
            if (schemaFrequency > 0) {
                Set<String> batch = new HashSet<>();
                int offset = 0;
                while (true) {
                    for (int i = offset; i < schemaFrequency && i - offset < BATCH_SIZE; i++)
                        batch.add(protocol.generateTrapdoorAndIncrementIV(schemaKeyword));
                    if (batch.isEmpty())
                        break;
                    response = deleteSomeContents(httpClient, protocolVersion, triplestoreID, batch, accessToken);
                    if (response.getStatus() != OK) {
                        deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                        return response.build();
                    }
                    deletions.add(response.getBody());
                    offset += batch.size();
                    batch.clear();
                }
            }
            Set<Triple> batch = new HashSet<>();
            while (!triples.isEmpty()) {
                for (int i = 0; i < BATCH_SIZE && !triples.isEmpty(); i++)
                    batch.add(triples.removeFirst());
                if (!batch.isEmpty())
                    batchOntologyUpload(httpClient, cookie, triplestoreID, protocol, accessToken, uploads, batch);
                protocol.clearNodes();
                batch.clear();
            }
            return updateTriplestore(httpClient, cookie, protocolVersion, triplestoreID, deletions, uploads, accessToken);
        } catch (Exception e) {
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            throw new RuntimeException(e);
        }
    }

    private void batchOntologyUpload(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID, EncryptionSchemeV2 protocol,
                                     String accessToken, List<String> uploads, Set<Triple> batch) throws InvalidNodeException, IOException {
        HTTPResponse response;
        protocol.encrypt(batch, true);
        response = upload(httpClient, protocolVersion, triplestoreID, protocol.getEncryptedNodes(), accessToken);
        if (response != null) {
            if (response.getStatus() != OK)
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            else
                uploads.add(response.getBody());
        }
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
            response = getSecrets(httpClient, triplestoreID, accessToken);
            if (response.getStatus() != OK) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }
            Map<String, String> secrets = ParsingUtils.parseSecretsMap(response.getBody());
            Ontology ontology = new DefaultOntology();
            response = fetchOntologySchema(httpClient, triplestoreID, getProtocol2(secrets), ontology, inference, accessToken);
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

    private HTTPResponse fetchOntologySchema(CloseableHttpClient httpClient,
                                             String triplestoreID, EncryptionSchemeV2 protocol,
                                             Ontology ontology, boolean inference, String accessToken) throws IOException, AEADBadTagException, ClassNotFoundException {
        String schemaKeyword = protocol.getSchemaKeyword();
        int numTrapdoors = rnd.nextInt(MIN_TRAPDOORS, MAX_TRAPDOORS);
        List<String> trapdoors = new ArrayList<>(numTrapdoors);
        int rndIndex = rnd.nextInt(0, numTrapdoors - 1);
        for (int i = 0; i < numTrapdoors; i++) {
            if (i == rndIndex)
                trapdoors.add(protocol.generateTrapdoor(schemaKeyword));
            else
                trapdoors.add(protocol.generateTrapdoor(generateID()));
        }
        HTTPResponse response = searchEncryptedTriplestoreContents(httpClient, protocolVersion, triplestoreID, trapdoors, accessToken);
        if (response.getStatus() != OK)
            return response;

        byte[] encryptedFrequency = ParsingUtils.parseListOfBytes(response.getBody()).get(rndIndex);
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

            List<byte[]> encryptedSchema = ParsingUtils.parseListOfBytes(response.getBody()).stream().filter(Objects::nonNull).toList();
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
            response = getSecrets(httpClient, triplestoreID, accessToken);
            if (response.getStatus() != OK) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }
            Map<String, String> secrets = ParsingUtils.parseSecretsMap(response.getBody());
            EncryptionSchemeV2 protocol = getProtocol2(secrets);
            SecureSPARQLPlanner planner;
            System.out.println("INFERENCE: " + form.getInference());
            if (form.getInference()) {
                Ontology ontology = new SecureOntology(form.getTransitivityDepth(), form.getExpansionDepth());
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

            if (queryType == INSERT_DATA || queryType == DELETE_DATA || queryType == DELETE_WHERE || queryType == MODIFY) {
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
                                       DefaultQueryExecutionPlan plan, EncryptionSchemeV2 protocol, String accessToken) throws InvalidNodeException, IOException, ClassNotFoundException, AEADBadTagException {
        System.out.println("QUERY TYPE: " + queryType);
        return switch (queryType) {
            case SELECT, ASK, DESCRIBE, CONSTRUCT ->
                    executeSPARQLQuery(httpClient, cookie, triplestoreID, queryType, planner, plan, protocol, accessToken);
            case INSERT_DATA, DELETE_DATA, DELETE_WHERE, MODIFY ->
                    executeSPARQLUpdateQuery(httpClient, cookie, triplestoreID, queryType, planner, plan, protocol, accessToken);
        };
    }

    private HTTPResponse fetchKeywordsFrequencies(HttpClient httpClient, String triplestoreID, List<String> keywords,
                                                  Map<String, Integer> keywordsFrequencyCollector, EncryptionSchemeV2 protocol, String accessToken) throws AEADBadTagException, IOException, ClassNotFoundException {
        List<String> trapdoors = new ArrayList<>(keywords.size());
        for (String keyword : keywords)
            trapdoors.add(protocol.generateTrapdoor(keyword));

        HTTPResponse response = searchEncryptedTriplestoreContents(httpClient, protocolVersion, triplestoreID, trapdoors, accessToken);
        if (response.getStatus() != OK)
            return response;
        List<byte[]> encryptedKeywordsFrequencies = ParsingUtils.parseListOfBytes(response.getBody());
        byte[] frequency;
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
                                        EncryptionSchemeV2 protocol, String accessToken) throws IOException, ClassNotFoundException, AEADBadTagException {
        Map<String, Integer> keywordsFrequency = new HashMap<>();
        List<String> keywords = planner.getKeywords().stream().toList();

        HTTPResponse response = fetchKeywordsFrequencies(httpClient, triplestoreID, keywords, keywordsFrequency, protocol, accessToken);

        if (response != null && response.getStatus() != OK) {
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return response.build();
        }
        BigInteger mask = DGKUtils.generateMask((DGKPublicKey) protocol.getPubDGK(), (DGKPrivateKey) protocol.getPrivDGK());
        BigInteger n = ((DGKPublicKey) protocol.getPubDGK()).getN();
        BigInteger inverseMask = mask.modPow(BigInteger.valueOf(-1), n);
        response = prepareSearches(httpClient, protocol, triplestoreID, planner.getSearchJobsIDs(), plan.getJobs(), keywordsFrequency,
                mask, n, accessToken);
        if (response != null && response.getStatus() != OK) {
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return response.build();
        }
        response = query(httpClient, protocolVersion, ParsingUtils.DGKKeyToByteArray(protocol.getEqKey()), plan, accessToken);
        if (response.getStatus() != OK) {
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return response.build();
        }
        SPARQLResult<byte[]> sparqlResult = ParsingUtils.parseSPARQLResult(response.getBody());
        Response res = null;
        if (queryType == SELECT || queryType == CONSTRUCT) {
            Map<Var, Var> deobfuscationMap = planner.getDeobfuscationMap();
            List<Var> vars = new LinkedList<>();
            for (Var var : plan.getVars())
                vars.add(deobfuscationMap.get(var));
            Collection<Binding> bindings = new LinkedList<>();
            response = fetchAndDecryptBindings(httpClient, triplestoreID, sparqlResult.getBindings(), deobfuscationMap, protocol, bindings,
                    inverseMask, n, accessToken);
            if (response != null && response.getStatus() != OK) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }
            if (sparqlResult.isOrdered())
                bindings = orderResults(sparqlResult.isDistinct(), sparqlResult.getSortConditions(), deobfuscationMap, bindings);
            if (sparqlResult.isSliced())
                bindings = sliceResults(sparqlResult.getOffset(), sparqlResult.getLength(), bindings);
            if (queryType == SELECT)
                res = generateSELECTResults(vars, bindings);
            else
                res = generateCONSTRUCTResults(planner.getConstructTemplate(), bindings);
        } else if (queryType == ASK)
            res = generateASKResults(sparqlResult);
        else if (queryType == DESCRIBE)
            res = generateDESCRIBEResults(plan.getVars(), planner.getDeobfuscationMap(), sparqlResult);
        deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
        return res;
    }


    private Response executeSPARQLUpdateQuery(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID, QueryType queryType, SecureSPARQLPlanner planner,
                                              DefaultQueryExecutionPlan plan, EncryptionSchemeV2 protocol, String accessToken) throws IOException {
        try {
            List<Triple> triplesToUpload, triplesToDelete;
            Map<String, Integer> keywordsFrequency = new HashMap<>();
            if (queryType == DELETE_WHERE || queryType == MODIFY) {
                HTTPResponse response = fetchKeywordsFrequencies(httpClient, triplestoreID,
                        planner.getKeywords().stream().toList(), keywordsFrequency, protocol, accessToken);
                if (response != null && response.getStatus() != OK) {
                    deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                    return response.build();
                }
                if (keywordsFrequency.containsValue(0)) {
                    deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                    return Response.ok(NO_UPDATES).build();
                }

                BigInteger mask = DGKUtils.generateMask((DGKPublicKey) protocol.getPubDGK(), (DGKPrivateKey) protocol.getPrivDGK());
                BigInteger n = ((DGKPublicKey) protocol.getPubDGK()).getN();
                BigInteger inverseMask = mask.modPow(BigInteger.valueOf(-1), n);
                response = prepareSearches(httpClient, protocol, triplestoreID, planner.getSearchJobsIDs(), plan.getJobs(), keywordsFrequency,
                        mask, n, accessToken);
                if (response != null && response.getStatus() != OK) {
                    deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                    return response.build();
                }
                response = query(httpClient, protocolVersion, ParsingUtils.DGKKeyToByteArray(protocol.getEqKey()), plan, accessToken);
                if (response.getStatus() != OK) {
                    deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                    return response.build();
                }
                SPARQLResult<byte[]> sparqlResult = parseSPARQLResult(response.getBody());
                Map<Var, Var> deobfuscationMap = planner.getDeobfuscationMap();
                Collection<Binding> bindings = new LinkedList<>();
                response = fetchAndDecryptBindings(httpClient, triplestoreID, sparqlResult.getBindings(), deobfuscationMap, protocol, bindings,
                        inverseMask, n, accessToken);
                if (response != null && response.getStatus() != OK) {
                    deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                    return response.build();
                }
                if (sparqlResult.isOrdered())
                    bindings = orderResults(sparqlResult.isDistinct(), sparqlResult.getSortConditions(), deobfuscationMap, bindings);
                if (sparqlResult.isSliced())
                    bindings = sliceResults(sparqlResult.getOffset(), sparqlResult.getLength(), bindings);
                if (bindings.isEmpty()) {
                    deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                    return Response.ok(NO_UPDATES).build();
                }
                triplesToUpload = QueryUtils.generateTriplesFromBindings(planner.getUploadTemplate(), bindings);
                triplesToDelete = QueryUtils.generateTriplesFromBindings(planner.getDeleteTemplate(), bindings);
                triplesToDelete.addAll(triplesToUpload);
                keywordsFrequency.clear();
            } else if (queryType == INSERT_DATA) {
                triplesToDelete = planner.getUploadTemplate();
                triplesToUpload = new LinkedList<>(planner.getUploadTemplate());
            } else {
                triplesToDelete = planner.getDeleteTemplate();
                triplesToUpload = new LinkedList<>();
            }

            System.out.println("Triples to Upload: " + triplesToUpload.size());
            System.out.println("Triples to Delete: " + triplesToDelete.size());
            HTTPResponse response;
            Set<Triple> batch = new HashSet<>();
            List<String> deletions = new LinkedList<>();
            List<String> uploads = new LinkedList<>();

            response = batchExecute(httpClient, cookie, triplestoreID, protocol, accessToken, (LinkedList<Triple>) triplesToDelete,
                    keywordsFrequency, batch, deletions, BatchOperation.DELETION);
            if (response != null && response.getStatus() != OK)
                return response.build();
            response = batchExecute(httpClient, cookie, triplestoreID, protocol, accessToken, (LinkedList<Triple>) triplesToUpload,
                    keywordsFrequency, batch, uploads, BatchOperation.UPLOAD);
            if (response != null && response.getStatus() != OK)
                return response.build();
            return updateTriplestore(httpClient, cookie, protocolVersion, triplestoreID, deletions, uploads, accessToken);
        } catch (Exception e) {
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            throw new RuntimeException(e);
        }
    }

    private HTTPResponse batchExecute(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID,
                                      EncryptionSchemeV2 protocol, String accessToken, LinkedList<Triple> triples,
                                      Map<String, Integer> keywordsFrequency, Set<Triple> batch, List<String> collector, BatchOperation opType) throws IOException, InvalidNodeException, AEADBadTagException, ClassNotFoundException {
        System.out.println("Triples: " + triples.size());
        HTTPResponse response = null;
        while (!triples.isEmpty()) {
            for (int i = 0; i < BATCH_SIZE && !triples.isEmpty(); i++)
                batch.add(triples.removeFirst());
            System.out.println("Batch: " + batch.size());
            System.out.println("Triples: " + triples.size());
            if (!batch.isEmpty())
                response = batch(httpClient, cookie, triplestoreID, protocol, accessToken, keywordsFrequency, batch, collector, opType);
            batch.clear();
            protocol.clearNodes();
            protocol.clearEqTags();
        }
        protocol.clearFrequencies();
        System.out.println("Check batch is empty" + batch.size());
        return response;
    }

    private HTTPResponse batch(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID, EncryptionSchemeV2 protocol, String accessToken,
                               Map<String, Integer> keywordsFrequency, Set<Triple> batch, List<String> collector, BatchOperation opType) throws IOException, InvalidNodeException, AEADBadTagException, ClassNotFoundException {
        HTTPResponse response;
        switch (opType) {
            case UPLOAD ->
                    response = prepareUploads(httpClient, triplestoreID, protocol, keywordsFrequency, batch, accessToken);
            case DELETION -> response = prepareDeletions(httpClient, triplestoreID, protocol, batch, accessToken);
            default -> throw new IllegalStateException("Unexpected value: " + opType);
        }
        if (response != null) {
            if (response.getStatus() != OK)
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            else
                collector.add(response.getBody());
        }
        return response;
    }

    private HTTPResponse prepareDeletions(CloseableHttpClient httpClient, String triplestoreID, EncryptionSchemeV2 protocol, Set<Triple> triplesToDelete,
                                          String accessToken) throws IOException, InvalidNodeException, AEADBadTagException, ClassNotFoundException {
        Map<String, List<String>> keywordsTrapdoors = protocol.generateKeywordsPatternTrapdoors(triplesToDelete);
        List<String> keywordList = new ArrayList<>(keywordsTrapdoors.keySet());
        List<String> trapdoors = new ArrayList<>(keywordsTrapdoors.values().stream().mapToInt(List::size).sum());
        for (String keyword : keywordList)
            trapdoors.addAll(keywordsTrapdoors.get(keyword));

        HTTPResponse response = searchEncryptedTriplestoreContents(httpClient, protocolVersion, triplestoreID, trapdoors, accessToken);
        if (response.getStatus() != OK)
            return response;
        List<byte[]> encryptedInstances = ParsingUtils.parseListOfBytes(response.getBody());

        byte[] encryptedInstance;
        int offset = 0;
        int length;
        Set<String> deletions = new HashSet<>();
        String eqTagTrapdoor;
        List<String> eqTagTrapdoors = new LinkedList<>();
        for (String keyword : keywordList) {
            length = keywordsTrapdoors.get(keyword).size();
            for (int i = offset; i < offset + length; i++) {
                encryptedInstance = encryptedInstances.get(i);
                if (encryptedInstance != null) {
                    eqTagTrapdoor = protocol.generateTrapdoor(keyword, ParsingUtils.byteArrayToInteger(protocol.decryptRNDLayer(encryptedInstance)));
                    deletions.add(eqTagTrapdoor);
                    deletions.add(trapdoors.get(i));
                    eqTagTrapdoors.add(eqTagTrapdoor);
                }
            }
            offset += length;
        }
        response = searchEncryptedTriplestoreContents(httpClient, protocolVersion, triplestoreID, eqTagTrapdoors, accessToken);
        if (response.getStatus() != OK)
            return response;

        System.out.println("DELETIONS: " + deletions.size());
        for (byte[] eqTag : ParsingUtils.parseListOfBytes(response.getBody()))
            if (eqTag != null)
                deletions.add(ParsingUtils.eqTagBytesToString(eqTag));

        if (!deletions.isEmpty()) {
            System.out.println("DELETIONS w/ eqTags: " + deletions.size());
            response = deleteSomeContents(httpClient, protocolVersion, triplestoreID, deletions, accessToken);
            return response;
        }
        return null;
    }

    private HTTPResponse prepareUploads(CloseableHttpClient httpClient, String triplestoreID, EncryptionSchemeV2 protocol,
                                        Map<String, Integer> keywordsFrequency, Set<Triple> triplesToUpload, String accessToken) throws IOException, InvalidNodeException, AEADBadTagException, ClassNotFoundException {

        HTTPResponse response = setKeywordFrequencies(httpClient, triplestoreID, protocol, keywordsFrequency, triplesToUpload, accessToken);
        if (response != null)
            return response;
        Map<String, Integer> eqTags = new HashMap<>(3 * triplesToUpload.size());
        response = fetchEqTags(httpClient, triplestoreID, triplesToUpload, protocol, eqTags, accessToken);
        if (response != null && response.getStatus() != OK)
            return response;
        protocol.setEqTags(eqTags);
        protocol.encrypt(triplesToUpload, false);
        Map<String, String> uploads = protocol.getEncryptedNodes();
        if (!uploads.isEmpty()) {
            System.out.println("Uploads: " + uploads.size());
            response = upload(httpClient, protocolVersion, triplestoreID, uploads, accessToken);
            return response;
        }
        return null;
    }

    private HTTPResponse setKeywordFrequencies(CloseableHttpClient httpClient, String triplestoreID, EncryptionSchemeV2 protocol, Map<String, Integer> keywordsFrequency,
                                               Set<Triple> triples, String accessToken) throws InvalidNodeException, AEADBadTagException, IOException, ClassNotFoundException {
        Set<String> keywords = ParsingUtils.generateKeywords(triples);
        keywords.removeAll(keywordsFrequency.keySet());
        if (!keywords.isEmpty()) {
            HTTPResponse response = fetchKeywordsFrequencies(httpClient, triplestoreID, keywords.stream().toList(), keywordsFrequency, protocol, accessToken);
            if (response != null && response.getStatus() != OK)
                return response;
        }
        protocol.setKeywordFrequencies(keywordsFrequency);
        return null;
    }

    private HTTPResponse fetchEqTags(CloseableHttpClient httpClient, String
            triplestoreID, Set<Triple> triples, EncryptionSchemeV2 protocol, Map<String, Integer> eqTagsCollector, String
                                             accessToken) throws InvalidNodeException, IOException, AEADBadTagException, ClassNotFoundException {
        List<String> eqTagTrapdoors = new LinkedList<>();
        List<String> nodes = new LinkedList<>();
        String s, p, o;
        Set<String> processed = new HashSet<>();
        for (Triple triple : triples) {
            s = ParsingUtils.parseNode(triple.getSubject());
            p = ParsingUtils.parseNode(triple.getPredicate());
            o = ParsingUtils.parseNode(triple.getObject());
            if (!processed.contains(s)) {
                nodes.add(s);
                eqTagTrapdoors.add(protocol.generateTrapdoor(s));
                processed.add(s);
            }
            if (!processed.contains(p)) {
                nodes.add(p);
                eqTagTrapdoors.add(protocol.generateTrapdoor(p));
                processed.add(p);
            }
            if (!processed.contains(o)) {
                nodes.add(o);
                eqTagTrapdoors.add(protocol.generateTrapdoor(o));
                processed.add(o);
            }
        }
        HTTPResponse response = searchEncryptedTriplestoreContents(httpClient, protocolVersion, triplestoreID, eqTagTrapdoors, accessToken);
        if (response.getStatus() != OK)
            return response;

        List<byte[]> encryptedEqTags = ParsingUtils.parseListOfBytes(response.getBody());
        byte[] encryptedEqTag;

        int i = 0;
        for (String node : nodes) {
            encryptedEqTag = encryptedEqTags.get(i);
            if (encryptedEqTag != null)
                eqTagsCollector.put(node, ParsingUtils.byteArrayToInteger(protocol.decryptRNDLayer(encryptedEqTag)));
            i++;
        }
        return null;
    }

    private HTTPResponse prepareSearches(CloseableHttpClient httpClient, EncryptionSchemeV2 protocol,
                                         String triplestoreID, Set<String> jobIDs, Map<String, Job> jobs,
                                         Map<String, Integer> keywordsFrequency, BigInteger mask, BigInteger n, String accessToken) throws
            IOException {
        HTTPResponse response;
        String keyword;
        SecureSearchJob secureSearchJob;
        List<List<String>> trapdoors;
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
                trapdoors = new ArrayList<>(vars.length);
                for (int i = 0; i < vars.length; i++)
                    trapdoors.add(new ArrayList<>(keywordFrequency / vars.length));
                for (int i = 0; i < keywordFrequency; i++)
                    trapdoors.get(i % vars.length).add(protocol.generateTrapdoorAndIncrementIV(keyword));
                for (int i = 0; i < vars.length; i++) {
                    response = prepareSearch(httpClient, protocolVersion, triplestoreID, trapdoors.get(i), mask, n, accessToken);
                    if (response.getStatus() != OK)
                        return response;
                    String searchID = response.getBody();
                    searchIDs.add(searchID);
                    secureSearchJob.prepareSearch(vars[i], searchID);
                    System.out.println("[ " + vars[i] + " ] - " + trapdoors.get(i).size() + " | " + searchID + " | " + keyword);
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

    private HTTPResponse fetchAndDecryptBindings(CloseableHttpClient httpClient, String triplestoreID,
                                                 Collection<SerializableBinding<byte[]>> bindings, Map<Var, Var> deobfuscationMap,
                                                 EncryptionSchemeV2 protocol, Collection<Binding> bindingsCollector,
                                                 BigInteger inverseMask, BigInteger n, String accessToken) throws AEADBadTagException, IOException, ClassNotFoundException {
        Map<BigInteger, Integer> eqTagsOrder = new HashMap<>();
        List<String> eqTags = new ArrayList<>();
        BigInteger eqTag;
        int i = 0;
        for (SerializableBinding<byte[]> binding : bindings) {
            for (Iterator<Var> it = binding.vars(); it.hasNext(); ) {
                eqTag = new BigInteger(binding.get(it.next()));
                if (!eqTagsOrder.containsKey(eqTag)) {
                    eqTagsOrder.put(eqTag, i);
                    eqTags.add(ParsingUtils.eqTagToString(eqTag.multiply(inverseMask).mod(n)));
                    i++;
                }
            }
        }

        HTTPResponse response = searchEncryptedTriplestoreContents(httpClient, protocolVersion, triplestoreID, eqTags, accessToken);
        if (response.getStatus() != OK) {
            return response;
        }
        List<byte[]> encryptedBindings = ParsingUtils.parseListOfBytes(response.getBody());
        BindingBuilder builder = Binding.builder();
        Map<Integer, Node> decryptedNodes = new HashMap<>();
        Node decryptedNode;

        Var var;
        for (SerializableBinding<byte[]> binding : bindings) {
            for (Iterator<Var> it = binding.vars(); it.hasNext(); ) {
                var = it.next();
                i = eqTagsOrder.get(new BigInteger(binding.get(var)));
                decryptedNode = decryptedNodes.get(i);
                if (decryptedNode == null) {
                    decryptedNode = generateNode(new String(protocol.decryptRNDLayer(encryptedBindings.get(i))));
                    decryptedNodes.put(i, decryptedNode);
                }
                builder.add(deobfuscationMap.get(var), decryptedNode);
            }
            bindingsCollector.add(builder.build());
            builder.reset();
        }
        return null;
    }

    public HTTPResponse prepareSearch(CloseableHttpClient httpClient, String protocolVersion, String
            triplestoreID, List<String> trapdoors, BigInteger mask, BigInteger n,
                                      String accessToken) throws IOException {
        try (CloseableHttpResponse response = EncryptedTriplestoreV2Client.prepareSearch(httpClient, protocolVersion, triplestoreID, trapdoors, mask, n, accessToken)) {
            return new HTTPResponse(response);
        }
    }
}
