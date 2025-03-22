package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
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
import pt.fct.nova.id.srv.application.crypto.schemes.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.crypto.schemes.EncryptionSchemeV1;
import pt.fct.nova.id.srv.application.query.QueryType;
import pt.fct.nova.id.srv.application.query.QueryUtils;
import pt.fct.nova.id.srv.application.query.execution.SPARQLResult;
import pt.fct.nova.id.srv.application.query.jobs.*;
import pt.fct.nova.id.srv.application.query.plans.DefaultQueryExecutionPlan;
import pt.fct.nova.id.srv.application.query.plans.SecureSPARQLPlanner;
import pt.fct.nova.id.srv.presentation.apis.EncryptedTriplestoreAPI;
import pt.fct.nova.id.srv.presentation.dtos.QueryForm;
import pt.fct.nova.id.srv.presentation.dtos.SchemaForm;
import pt.fct.nova.id.srv.presentation.dtos.TriplestoreForm;
import pt.fct.nova.id.srv.presentation.dtos.UploadForm;
import pt.fct.nova.id.srv.presentation.exceptions.UnknownRDFLanguageException;

import javax.crypto.AEADBadTagException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

import static jakarta.ws.rs.core.Response.Status.*;
import static pt.fct.nova.id.srv.application.query.QueryType.*;
import static pt.fct.nova.id.srv.presentation.controllers.ParsingUtils.*;
import static pt.fct.nova.id.srv.presentation.controllers.TriplestoreController.*;

@Path("triplestores/encrypted/v1")
public class EncryptedTriplestoreV1Controller extends EncryptedTriplestoreController implements EncryptedTriplestoreAPI {
    private final String protocolVersion = "v1";

    @Override
    public Response create(Cookie cookie, TriplestoreForm form) {
        if (cookie == null)
            return Response.status(BAD_REQUEST).build();
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            String triplestoreID = form.getTriplestoreID();
            String issuer = form.getIssuer();
            HTTPResponse response = createTriplestoreAccessList(httpClient, cookie, triplestoreID, issuer);
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
            response = saveSecrets(httpClient, triplestoreID, generateSecretsMap(new EncryptionSchemeV1()), accessToken);
            if (response.getStatus() != OK) {
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
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response delete(Cookie cookie, String triplestoreID, String issuer) {
        if (cookie == null)
            return Response.status(BAD_REQUEST).build();
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            HTTPResponse response = deleteEncryptedTriplestore(httpClient, cookie, protocolVersion, triplestoreID, issuer);
            if (response.getStatus() != OK)
                return response.build();
            return Response.ok(SUCCESSFUL_DELETION).build();
        } catch (Exception e) {
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response upload(Cookie cookie, boolean schema, UploadForm form) {
        if (cookie == null)
            return Response.status(BAD_REQUEST).build();
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            String issuer = form.getIssuer();
            String triplestoreID = form.getTriplestoreID();
            if (form.getContent() == null)
                return Response.ok(EMPTY_UPLOAD).status(BAD_REQUEST).build();
            Set<Triple> triples = parseTriples(form.getContent(), parseRDFLanguage(form.getSyntax()));
            if (triples.isEmpty())
                return Response.ok(EMPTY_UPLOAD).status(BAD_REQUEST).build();
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
            EncryptionSchemeV1 protocol = getProtocol1(secrets);

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
            return Response.ok(INVALID_SYNTAX).status(BAD_REQUEST).build();
        } catch (InvalidNodeException e) {
            return Response.ok(BAD_NODE).status(BAD_REQUEST).build();
        } catch (Exception e) {
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }
    }

    private Response uploadOntologySchema(CloseableHttpClient httpClient, Cookie cookie,
                                          String triplestoreID, EncryptionSchemeV1 protocol, LinkedList<Triple> triples,
                                          String accessToken) throws IOException {
        try {
            String schemaKeyword = protocol.getSchemaKeyword();
            List<String> trapdoors = List.of(protocol.generateTrapdoor(schemaKeyword));
            HTTPResponse response = searchEncryptedTriplestoreContents(httpClient, protocolVersion, triplestoreID, trapdoors, accessToken);
            if (response.getStatus() != OK) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }
            byte[] encryptedFrequency = ParsingUtils.parseListOfBytes(response.getBody()).get(0);
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
                protocol.clearFrequencies();
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

    private void batchOntologyUpload(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID, EncryptionSchemeV1 protocol, String accessToken,
                                     List<String> uploads, Set<Triple> batch) throws InvalidNodeException, IOException {
        HTTPResponse response;
        protocol.encrypt(batch, true);
        response = upload(httpClient, protocolVersion, triplestoreID, protocol.getEncryptedNodes(), accessToken);
        if (response != null) {
            if (response.getStatus() != OK) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            } else
                uploads.add(response.getBody());
        }
    }

    @Override
    public Response fetchSchema(Cookie cookie, boolean inference, SchemaForm form) {
        if (cookie == null)
            return Response.status(BAD_REQUEST).build();
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
            response = fetchOntologySchema(httpClient, cookie, triplestoreID, getProtocol1(secrets), ontology, inference, accessToken);
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
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }
    }

    private HTTPResponse fetchOntologySchema(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID, EncryptionSchemeV1 protocol,
                                             Ontology ontology, boolean inference, String accessToken) throws IOException {
        try {
            String schemaKeyword = protocol.getSchemaKeyword();
            List<String> trapdoors = List.of(protocol.generateTrapdoor(schemaKeyword));
            HTTPResponse response = searchEncryptedTriplestoreContents(httpClient, protocolVersion, triplestoreID, trapdoors, accessToken);
            if (response.getStatus() != OK)
                return response;
            byte[] encryptedFrequency = ParsingUtils.parseListOfBytes(response.getBody()).get(0);
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
        } catch (Exception e) {
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Response answerSPARQLQuery(Cookie cookie, QueryForm form) {
        if (cookie == null)
            return Response.status(BAD_REQUEST).build();
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
            EncryptionSchemeV1 protocol = getProtocol1(secrets);
            SecureSPARQLPlanner planner;
            System.out.println("INFERENCE: " + form.getInference());
            if (form.getInference()) {
                Ontology ontology = new SecureOntology(form.getTransitivityDepth(), form.getExpansionDepth());
                response = fetchOntologySchema(httpClient, cookie, triplestoreID, protocol, ontology, form.getInference(), accessToken);
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
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }
    }

    private Response answerSPARQLQuery(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID, QueryType queryType, SecureSPARQLPlanner planner,
                                       DefaultQueryExecutionPlan plan, EncryptionSchemeV1 protocol, String accessToken) throws InvalidNodeException,
            IOException, URISyntaxException, ClassNotFoundException, AEADBadTagException {
        System.out.println("QUERY TYPE: " + queryType);
        return switch (queryType) {
            case SELECT, ASK, DESCRIBE, CONSTRUCT ->
                    executeSPARQLQuery(httpClient, cookie, triplestoreID, queryType, planner, plan, protocol, accessToken);
            case INSERT_DATA, DELETE_DATA, DELETE_WHERE, MODIFY ->
                    executeSPARQLUpdateQuery(httpClient, cookie, triplestoreID, queryType, planner, plan, protocol, accessToken);
        };
    }

    private HTTPResponse fetchKeywordsFrequencies(HttpClient httpClient, String triplestoreID, List<String> keywords,
                                                  Map<String, Integer> keywordsFrequencyCollector, EncryptionSchemeV1 protocol,
                                                  String accessToken) throws AEADBadTagException, IOException, ClassNotFoundException {
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
            if (frequency != null) {
                keywordsFrequencyCollector.put(keywords.get(i), ParsingUtils.byteArrayToInteger(protocol.decryptRNDLayer(frequency)));
            } else
                keywordsFrequencyCollector.put(keywords.get(i), 0);
        }
        return null;
    }

    private Response executeSPARQLQuery(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID,
                                        QueryType queryType, SecureSPARQLPlanner planner, DefaultQueryExecutionPlan plan,
                                        EncryptionSchemeV1 protocol, String accessToken) throws IOException, URISyntaxException, ClassNotFoundException, AEADBadTagException {
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
        SPARQLResult<byte[]> sparqlResult = ParsingUtils.parseSPARQLResult(response.getBody());
        Response res = null;
        if (queryType == SELECT || queryType == CONSTRUCT) {
            Map<Var, Var> deobfuscationMap = planner.getDeobfuscationMap();
            List<Var> vars = new LinkedList<>();
            for (Var var : plan.getVars())
                vars.add(deobfuscationMap.get(var));
            Collection<Binding> bindings = decryptBindings(sparqlResult.getBindings(), deobfuscationMap, protocol);
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
                                              DefaultQueryExecutionPlan plan, EncryptionSchemeV1 protocol, String accessToken) throws IOException {
        try {
            List<Triple> triplesToUpload, triplesToDelete;
            Map<String, Integer> keywordsFrequency = new HashMap<>();
            if (queryType == DELETE_WHERE || queryType == MODIFY) {
                HTTPResponse response;
                response = fetchKeywordsFrequencies(httpClient, triplestoreID,
                        planner.getKeywords().stream().toList(), keywordsFrequency, protocol, accessToken);

                if (response != null && response.getStatus() != OK) {
                    deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                    return response.build();
                }
                if (keywordsFrequency.containsValue(0)) {
                    deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                    return Response.ok(NO_UPDATES).build();
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
                Map<Var, Var> deobfuscationMap = planner.getDeobfuscationMap();
                SPARQLResult<byte[]> sparqlResult = parseSPARQLResult(response.getBody());
                Collection<Binding> bindings = decryptBindings(sparqlResult.getBindings(), deobfuscationMap, protocol);
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
            System.out.println("Deletion IDs: " + Arrays.toString(deletions.toArray()));
            System.out.println("Uploads IDs: " + Arrays.toString(uploads.toArray()));
            return updateTriplestore(httpClient, cookie, protocolVersion, triplestoreID, deletions, uploads, accessToken);
        } catch (Exception e) {
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            throw new RuntimeException(e);
        }
    }

    private HTTPResponse batchExecute(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID,
                                      EncryptionSchemeV1 protocol, String accessToken, LinkedList<Triple> triples,
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
        }
        protocol.clearFrequencies();
        System.out.println("Check batch is empty" + batch.size());
        return response;
    }

    private HTTPResponse batch(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID, EncryptionSchemeV1 protocol,
                               String accessToken, Map<String, Integer> keywordsFrequency, Set<Triple> batch,
                               List<String> collector, BatchOperation opType) throws IOException, InvalidNodeException, AEADBadTagException, ClassNotFoundException {
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

    private HTTPResponse prepareDeletions(CloseableHttpClient httpClient, String triplestoreID, EncryptionSchemeV1 protocol,
                                          Set<Triple> triplesToDelete, String accessToken) throws IOException, InvalidNodeException, AEADBadTagException, ClassNotFoundException {
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
        for (String keyword : keywordList) {
            length = keywordsTrapdoors.get(keyword).size();
            for (int i = offset; i < offset + length; i++) {
                encryptedInstance = encryptedInstances.get(i);
                if (encryptedInstance != null) {
                    deletions.add(protocol.generateTrapdoor(keyword, ParsingUtils.byteArrayToInteger(protocol.decryptRNDLayer(encryptedInstance))));
                    deletions.add(trapdoors.get(i));
                }
            }
            offset += length;
        }

        if (!deletions.isEmpty()) {
            System.out.println("DELETIONS: " + deletions.size());
            response = deleteSomeContents(httpClient, protocolVersion, triplestoreID, deletions, accessToken);
            return response;
        }
        return null;
    }

    private HTTPResponse prepareUploads(CloseableHttpClient httpClient, String triplestoreID, EncryptionSchemeV1 protocol,
                                        Map<String, Integer> keywordsFrequency, Set<Triple> triplesToUpload, String accessToken) throws IOException, InvalidNodeException, AEADBadTagException, ClassNotFoundException {
        HTTPResponse response = setKeywordFrequencies(httpClient, triplestoreID, protocol, keywordsFrequency, triplesToUpload, accessToken);
        if (response != null)
            return response;
        protocol.encrypt(triplesToUpload, false);
        Map<String, String> uploads = protocol.getEncryptedNodes();
        if (!uploads.isEmpty()) {
            System.out.println("Uploads: " + uploads.size());
            response = upload(httpClient, protocolVersion, triplestoreID, uploads, accessToken);
            return response;
        }
        return null;
    }

    private HTTPResponse setKeywordFrequencies(CloseableHttpClient httpClient, String triplestoreID, EncryptionSchemeV1 protocol,
                                               Map<String, Integer> keywordsFrequency, Set<Triple> triples, String accessToken) throws InvalidNodeException, AEADBadTagException, IOException, ClassNotFoundException {
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

    private HTTPResponse prepareSearches(CloseableHttpClient httpClient, EncryptionSchemeV1 protocol,
                                         String triplestoreID, Set<String> jobIDs, Map<String, Job> jobs,
                                         Map<String, Integer> keywordsFrequency, String accessToken) throws IOException, URISyntaxException {
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
                    response = prepareSearch(httpClient, triplestoreID, trapdoors.get(i), accessToken);
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

    private Collection<Binding> decryptBindings(Collection<SerializableBinding<byte[]>> bindings, Map<Var, Var> deobfuscationMap, EncryptionSchemeV1 protocol) throws AEADBadTagException {
        Collection<Binding> decryptedBindings = new LinkedList<>();
        BindingBuilder builder = Binding.builder();
        for (SerializableBinding<byte[]> binding : bindings) {
            for (Iterator<Var> it = binding.vars(); it.hasNext(); ) {
                Var var = it.next();
                builder.add(deobfuscationMap.get(var), generateNode(new String(protocol.decryptDETLayer(binding.get(var)))));
            }
            decryptedBindings.add(builder.build());
            builder.reset();
        }
        return decryptedBindings;
    }

    private HTTPResponse prepareSearch(CloseableHttpClient httpClient, String triplestoreID, List<String> trapdoors, String accessToken) throws IOException, URISyntaxException {
        try (CloseableHttpResponse response = EncryptedTriplestoreV1Client.prepareSearch(httpClient, protocolVersion, triplestoreID, trapdoors, accessToken)) {
            return new HTTPResponse(response);
        }
    }

}
