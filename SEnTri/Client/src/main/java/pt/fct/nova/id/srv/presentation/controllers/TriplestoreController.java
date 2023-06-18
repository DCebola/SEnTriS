package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ResultSetStream;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingBuilder;
import org.json.JSONObject;
import pt.fct.nova.id.srv.application.ontologies.DefaultOntology;
import pt.fct.nova.id.srv.application.ontologies.Ontology;
import pt.fct.nova.id.srv.application.query.SPARQLQueryEngine;
import pt.fct.nova.id.srv.application.clients.*;
import pt.fct.nova.id.srv.application.protocols.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.query.QueryType;
import pt.fct.nova.id.srv.application.query.QueryUtils;
import pt.fct.nova.id.srv.application.query.execution.SPARQLResult;
import pt.fct.nova.id.srv.application.query.jobs.SerializableBinding;
import pt.fct.nova.id.srv.application.query.plans.DefaultQueryExecutionPlan;
import pt.fct.nova.id.srv.application.query.plans.DefaultSPARQLPlanner;
import pt.fct.nova.id.srv.presentation.api.TriplestoreAPI;
import pt.fct.nova.id.srv.presentation.api.dtos.QueryForm;
import pt.fct.nova.id.srv.presentation.api.dtos.SchemaForm;
import pt.fct.nova.id.srv.presentation.api.dtos.TriplestoreForm;
import pt.fct.nova.id.srv.presentation.api.dtos.UploadForm;
import pt.fct.nova.id.srv.presentation.exceptions.UnknownRDFLanguageException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.*;

import static jakarta.ws.rs.core.Response.Status.*;
import static pt.fct.nova.id.srv.application.query.QueryType.*;
import static pt.fct.nova.id.srv.presentation.controllers.EncryptedTriplestoreV1Controller.*;
import static pt.fct.nova.id.srv.presentation.controllers.ParsingUtils.*;


@Path("triplestores")
public class TriplestoreController implements TriplestoreAPI {
    public static final String SUCCESSFUL_DELETION = "Successful deletion.";
    public static final String INTERNAL_ERROR = "Internal error.";
    public static final String INVALID_SYNTAX = "Invalid syntax.";
    static final String BAD_NODE = "Data must only contain concrete nodes: IRI, Blank, Literal.";
    public static final String NOT_IMPLEMENTED_ERROR = "Operation not yet supported.";
    public static final String INVALID_COOKIE = "Malformed cookie.";
    public static final String SUCCESSFUL_UPDATE = "Successful update.";

    @Override
    public Response create(Cookie cookie, TriplestoreForm form) {
        if (cookie == null)
            return Response.ok(INVALID_COOKIE).status(BAD_REQUEST).build();
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            HTTPResponse response = createTriplestoreAccessPolicy(httpClient, cookie, form.getTriplestoreID(), form.getIssuer());
            if (response.getStatus() != OK)
                return response.build();
            return Response.ok(SUCCESSFUL_CREATION).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
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
            response = fetchSchema(httpClient, triplestoreID, accessToken);
            if (response.getStatus() != OK) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }
            Ontology ontology = new DefaultOntology(triplestoreID);
            ontology.execInference(ParsingUtils.parseSchema(response.getBody()), inference);
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

    @Override
    public Response listTriplestores(Cookie cookie, String issuer, boolean write, boolean read, boolean owns) {
        if (cookie == null)
            return Response.ok(INVALID_COOKIE).status(BAD_REQUEST).build();
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            return listTriplestores(httpClient, cookie, issuer, write, read, owns).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response info(Cookie cookie, String triplestoreID, String issuer) {
        if (cookie == null)
            return Response.ok(INVALID_COOKIE).status(BAD_REQUEST).build();
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            HTTPResponse response = createAccessToken(httpClient, cookie, issuer, triplestoreID);
            if (response.getStatus() != OK)
                return response.build();
            String accessToken = response.getBody();
            response = fetchTriplestoreInfo(httpClient, triplestoreID, accessToken);
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return response.build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }



    @Override
    public Response upload(Cookie cookie, boolean schema, UploadForm form) {
        if (cookie == null)
            return Response.ok(INVALID_COOKIE).status(BAD_REQUEST).build();
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            String triplestoreID = form.getTriplestoreID();
            InputStream contents = form.getContents();
            if (contents == null)
                return Response.ok(EMPTY_UPLOAD).status(BAD_REQUEST).build();
            Set<Triple> triples = new HashSet<>(parseTriples(contents, parseRDFLanguage(form.getSyntax())));
            if (!triples.isEmpty()) {
                HTTPResponse response = createAccessToken(httpClient, cookie, form.getIssuer(), triplestoreID);
                if (response.getStatus() != OK)
                    return response.build();
                return upload(httpClient, cookie, triplestoreID, triples, schema, response.getBody());
            }
            return Response.ok(EMPTY_UPLOAD).status(BAD_REQUEST).build();
        } catch (UnknownRDFLanguageException e) {
            return Response.ok(INVALID_SYNTAX).status(BAD_REQUEST).build();
        } catch (InvalidNodeException e) {
            return Response.ok(BAD_NODE).status(BAD_REQUEST).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    private Response upload(HttpClient httpClient, Cookie cookie, String triplestoreID, Set<Triple> triples, boolean schema, String accessToken) throws IOException, URISyntaxException {
        HTTPResponse response = acquireTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
        if (response.getStatus() != OK) {
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return response.build();
        }
        response = upload(httpClient, triplestoreID, triples, schema, accessToken);
        releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
        deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
        return response.build();
    }


    @Override
    public Response delete(Cookie cookie, String triplestoreID, String issuer, boolean schema) {
        if (cookie == null)
            return Response.ok(INVALID_COOKIE).status(BAD_REQUEST).build();
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            HTTPResponse response = createAccessToken(httpClient, cookie, issuer, triplestoreID);
            if (response.getStatus() != OK)
                return response.build();
            String accessToken = response.getBody();

            response = acquireTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
            if (response.getStatus() != OK) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }

            response = deleteTriplestoreContents(httpClient, triplestoreID, schema, accessToken);
            if (response.getStatus() != OK) {
                releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }
            if (!schema) {
                response = deleteTriplestoreAccessPolicy(httpClient, cookie, triplestoreID, accessToken);
                if (response.getStatus() != OK) {
                    releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                    deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                    return response.build();
                }
            } else {
                releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            }
            return Response.ok(SUCCESSFUL_DELETION).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
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
            response = fetchSchema(httpClient, triplestoreID, accessToken);
            if (response.getStatus() != OK) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }
            DefaultSPARQLPlanner planner;
            if (form.getInference()) {
                Ontology ontology = new DefaultOntology(triplestoreID, form.getTransitivityDepth(), form.getExpansionDepth());
                ontology.execInference(ParsingUtils.parseSchema(response.getBody()), form.getInference());
                planner = new DefaultSPARQLPlanner(ontology);
            } else
                planner = new DefaultSPARQLPlanner();
            DefaultQueryExecutionPlan plan = (DefaultQueryExecutionPlan) new SPARQLQueryEngine(planner).getQueryPlan(form.getQuery());
            QueryType queryType = planner.getQueryType();

            return switch (queryType) {
                case SELECT, ASK, DESCRIBE, CONSTRUCT ->
                        executeSPARQLQuery(httpClient, cookie, triplestoreID, queryType, planner, plan, accessToken);
                case INSERT_DATA, DELETE_DATA, DELETE_WHERE, MODIFY ->
                        executeSPARQLUpdateQuery(httpClient, cookie, triplestoreID, queryType, planner, plan, accessToken);
            };
        } catch (NotImplemented e) {
            return Response.ok(NOT_IMPLEMENTED_ERROR).status(INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }


    private Response executeSPARQLQuery(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID, QueryType queryType,
                                        DefaultSPARQLPlanner planner, DefaultQueryExecutionPlan plan, String accessToken) throws IOException, ClassNotFoundException {
        HTTPResponse response = query(httpClient, triplestoreID, plan, accessToken);
        if (response.getStatus() != OK)
            return response.build();
        SPARQLResult<String> sparqlResult = parseSPARQLResult(response.getBody());
        Response res = null;
        if (queryType == SELECT) res = generateSELECTResults(plan.getVars(), sparqlResult);
        else if (queryType == CONSTRUCT) res = generateCONSTRUCTResults(planner.getConstructTemplate(), sparqlResult);
        else if (queryType == ASK) res = generateASKResults(sparqlResult);
        else if (queryType == DESCRIBE) res = generateDESCRIBEResults(plan.getVars(), sparqlResult);
        deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
        return res;
    }

    private Response executeSPARQLUpdateQuery(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID,
                                              QueryType queryType, DefaultSPARQLPlanner planner, DefaultQueryExecutionPlan plan, String accessToken) throws IOException, ClassNotFoundException, URISyntaxException {
        HTTPResponse response = acquireTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
        if (response.getStatus() != OK) {
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return response.build();
        }
        Set<Triple> triplesToUpload = new HashSet<>();
        Set<Triple> triplesToDelete = new HashSet<>();
        if (queryType == MODIFY || queryType == DELETE_WHERE) {
            response = query(httpClient, triplestoreID, plan, accessToken);
            if (response.getStatus() != OK) {
                releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }
            SPARQLResult<String> sparqlResult = parseSPARQLResult(response.getBody());
            if (queryType == MODIFY)
                triplesToUpload = QueryUtils.generateTriplesFromSerializableBindings(planner.getUploadTemplate(), sparqlResult.getBindings());
            triplesToDelete = QueryUtils.generateTriplesFromSerializableBindings(planner.getDeleteTemplate(), sparqlResult.getBindings());
        } else if (queryType == INSERT_DATA)
            triplesToUpload = planner.getUploadTemplate();
        else if (queryType == DELETE_DATA)
            triplesToDelete = planner.getDeleteTemplate();
        return updateTriplestore(httpClient, cookie, triplestoreID, triplesToUpload, triplesToDelete, accessToken);
    }

    private Response updateTriplestore(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID,
                                       Set<Triple> triplesToUpload, Set<Triple> triplesToDelete, String accessToken) throws IOException, URISyntaxException {
        HTTPResponse response;
        if (!triplesToDelete.isEmpty()) {
            response = deleteSome(httpClient, triplestoreID, triplesToDelete, accessToken);
            if (response.getStatus() != OK) {
                releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }
        }
        if (!triplesToUpload.isEmpty()) {
            response = upload(httpClient, triplestoreID, triplesToUpload, false, accessToken);
            if (response.getStatus() != OK) {
                releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }
        }
        releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
        deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
        return Response.ok(SUCCESSFUL_UPDATE).build();
    }

    public static Response generateASKResults(SPARQLResult<String> sparqlResult) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ResultSetFormatter.outputAsJSON(out, !sparqlResult.getBindings().isEmpty());
            return Response.ok(out.toByteArray()).build();
        }
    }

    private Response generateCONSTRUCTResults(List<Triple> constructTemplate, SPARQLResult<String> sparqlResult) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Graph g = QueryUtils.generateGraphFromSerializableBindings(constructTemplate, sparqlResult.getBindings());
            RDFWriter.create(g).lang(Lang.JSONLD11).output(out);
            return Response.ok(out.toByteArray()).build();
        }
    }

    private Response generateSELECTResults(List<Var> vars, SPARQLResult<String> sparqlResult) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Collection<Binding> bindings = new LinkedList<>();
            BindingBuilder builder = Binding.builder();
            for (SerializableBinding<String> binding : sparqlResult.getBindings()) {
                for (Iterator<Var> it = binding.vars(); it.hasNext(); ) {
                    Var var = it.next();
                    builder.add(var, ParsingUtils.generateNode(binding.get(var)));
                }
                bindings.add(builder.build());
                builder.reset();
            }
            ResultSetFormatter.outputAsJSON(out, ResultSetStream.create(vars, bindings.iterator()));
            return Response.ok(out.toByteArray()).build();
        }
    }

    private Response generateDESCRIBEResults(List<Var> vars, SPARQLResult<String> sparqlResult) {
        Map<Var, Integer> frequencies = new HashMap<>();
        for (Var v : vars)
            frequencies.put(v, 0);
        for (SerializableBinding<String> binding : sparqlResult.getBindings()) {
            for (Iterator<Var> it = binding.vars(); it.hasNext(); ) {
                Var v = it.next();
                frequencies.put(v, frequencies.get(v) + 1);
            }
        }
        JSONObject responseBody = new JSONObject();
        for (Var v : vars)
            responseBody = responseBody.put(v.getVarName(), frequencies.get(v));
        return Response.ok(responseBody.toString()).build();
    }


    @Override
    public Response updateTriplestoreOwner(Cookie cookie, String triplestoreID, String issuer, String target) {
        if (cookie == null)
            return Response.ok(INVALID_COOKIE).status(BAD_REQUEST).build();
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            HTTPResponse response = createAccessToken(httpClient, cookie, issuer, triplestoreID);
            if (response.getStatus() != OK)
                return response.build();
            String accessToken = response.getBody();

            response = acquireTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
            if (response.getStatus() != OK) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }

            response = updateTriplestoreOwner(httpClient, cookie, triplestoreID, target, accessToken);
            releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return response.build();
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response issueAccessRequest(Cookie cookie, String triplestoreID, String issuer, boolean write) {
        if (cookie == null)
            return Response.ok(INVALID_COOKIE).status(BAD_REQUEST).build();
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            return requestAccess(httpClient, cookie, triplestoreID, issuer, write).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response listPendingAccessRequests(Cookie cookie, String triplestoreID, String issuer) {
        if (cookie == null)
            return Response.ok(INVALID_COOKIE).status(BAD_REQUEST).build();
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            HTTPResponse response = createAccessToken(httpClient, cookie, issuer, triplestoreID);
            if (response.getStatus() != OK)
                return response.build();
            String accessToken = response.getBody();

            response = listPendingAccessRequests(httpClient, cookie, triplestoreID, accessToken);
            IAMClient.deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return response.build();
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }


    @Override
    public Response processPendingAccessRequest(Cookie cookie, String triplestoreID, String issuer, String requestID, boolean accept) {
        if (cookie == null)
            return Response.ok(INVALID_COOKIE).status(BAD_REQUEST).build();
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            HTTPResponse response = createAccessToken(httpClient, cookie, issuer, triplestoreID);
            if (response.getStatus() != OK)
                return response.build();
            String accessToken = response.getBody();
            response = processAccessRequest(httpClient, cookie, triplestoreID, requestID, accept, accessToken);
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return response.build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response grantAccess(Cookie cookie, String triplestoreID, String issuer, String target, boolean write) {
        if (cookie == null)
            return Response.ok(INVALID_COOKIE).status(BAD_REQUEST).build();
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            HTTPResponse response = createAccessToken(httpClient, cookie, issuer, triplestoreID);
            if (response.getStatus() != OK)
                return response.build();
            String accessToken = response.getBody();

            response = acquireTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
            if (response.getStatus() != OK) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }

            response = grantAccess(httpClient, cookie, triplestoreID, target, write, accessToken);
            releaseTriplestoreLock(httpClient, cookie, issuer, triplestoreID);
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return response.build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response revokeAccess(Cookie cookie, String triplestoreID, String issuer, String target, boolean write) {
        if (cookie == null)
            return Response.ok(INVALID_COOKIE).status(BAD_REQUEST).build();
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            HTTPResponse response = createAccessToken(httpClient, cookie, issuer, triplestoreID);
            if (response.getStatus() != OK)
                return response.build();
            String accessToken = response.getBody();

            response = acquireTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
            if (response.getStatus() != OK) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }

            response = revokeAccess(httpClient, cookie, triplestoreID, target, write, accessToken);
            releaseTriplestoreLock(httpClient, cookie, issuer, triplestoreID);
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return response.build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response listUsersWithAccess(Cookie cookie, String triplestoreID, String issuer, boolean write) {
        if (cookie == null)
            return Response.ok(INVALID_COOKIE).status(BAD_REQUEST).build();
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            HTTPResponse response = createAccessToken(httpClient, cookie, issuer, triplestoreID);
            if (response.getStatus() != OK)
                return response.build();
            String accessToken = response.getBody();

            response = listUsersWithAccess(httpClient, cookie, triplestoreID, write, accessToken);
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return response.build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    public static HTTPResponse createTriplestoreAccessPolicy(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID, String issuer) throws IOException {
        try (CloseableHttpResponse response = IAMClient.createTriplestore(httpClient, cookie, triplestoreID, issuer)) {
            return new HTTPResponse(response);
        }
    }


    public static HTTPResponse deleteTriplestoreAccessPolicy(HttpClient httpClient, Cookie cookie, String triplestoreID, String accessToken) throws IOException {
        try (CloseableHttpResponse response = IAMClient.deleteTriplestore(httpClient, cookie, triplestoreID, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    private HTTPResponse deleteTriplestoreContents(HttpClient httpClient, String triplestoreID, boolean schema, String accessToken) throws IOException, URISyntaxException {
        try (CloseableHttpResponse response = TriplestoreClient.deleteAll(httpClient, triplestoreID, schema, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    private HTTPResponse listTriplestores(CloseableHttpClient httpClient, Cookie cookie, String issuer, boolean write, boolean read, boolean owns) throws IOException, URISyntaxException {
        try (CloseableHttpResponse response = IAMClient.listTriplestores(httpClient, cookie, issuer, write, read, owns)) {
            return new HTTPResponse(response);
        }
    }

    private HTTPResponse fetchTriplestoreInfo(CloseableHttpClient httpClient, String triplestoreID, String accessToken) throws IOException {
        try (CloseableHttpResponse response = TriplestoreClient.fetchInfo(httpClient, triplestoreID, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    public static HTTPResponse createAccessToken(CloseableHttpClient httpClient, Cookie cookie, String issuer, String triplestoreID) throws IOException {
        try (CloseableHttpResponse response = IAMClient.createAccessToken(httpClient, cookie, issuer, triplestoreID)) {
            return new HTTPResponse(response);
        }
    }

    public static HTTPResponse acquireTriplestoreLock(HttpClient httpClient, Cookie cookie, String triplestoreID, String accessToken) throws IOException {
        try (CloseableHttpResponse response = IAMClient.acquireTriplestoreLock(httpClient, cookie, triplestoreID, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    public static HTTPResponse releaseTriplestoreLock(HttpClient httpClient, Cookie cookie, String triplestoreID, String accessToken) throws IOException {
        try (CloseableHttpResponse response = IAMClient.releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    public static HTTPResponse deleteAccessToken(HttpClient httpClient, Cookie cookie, String triplestoreID, String accessToken) throws IOException {
        try (CloseableHttpResponse response = IAMClient.deleteAccessToken(httpClient, cookie, triplestoreID, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    private HTTPResponse fetchSchema(CloseableHttpClient httpClient, String triplestoreID, String accessToken) throws IOException {
        try (CloseableHttpResponse response = TriplestoreClient.fetchSchema(httpClient, triplestoreID, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    private HTTPResponse upload(HttpClient httpClient, String triplestoreID, Set<Triple> triples, boolean schema, String accessToken) throws IOException, URISyntaxException {
        try (CloseableHttpResponse response = TriplestoreClient.upload(httpClient, triplestoreID, triples, schema, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    private HTTPResponse deleteSome(HttpClient httpClient, String triplestoreID, Set<Triple> triples, String accessToken) throws IOException, URISyntaxException {
        try (CloseableHttpResponse response = TriplestoreClient.deleteSome(httpClient, triplestoreID, triples, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    private HTTPResponse query(CloseableHttpClient httpClient, String triplestoreID, DefaultQueryExecutionPlan plan, String accessToken) throws IOException {
        try (CloseableHttpResponse response = TriplestoreClient.query(httpClient, triplestoreID, plan, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    private HTTPResponse updateTriplestoreOwner(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID, String target, String accessToken) throws IOException {
        try (CloseableHttpResponse response = IAMClient.updateTriplestoreOwner(httpClient, cookie, triplestoreID, target, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    private HTTPResponse revokeAccess(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID, String target, boolean write, String accessToken) throws IOException, URISyntaxException {
        try (CloseableHttpResponse response = IAMClient.revokeAccess(httpClient, cookie, triplestoreID, target, write, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    private HTTPResponse grantAccess(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID, String target, boolean write, String accessToken) throws IOException, URISyntaxException {
        try (CloseableHttpResponse response = IAMClient.grantAccess(httpClient, cookie, triplestoreID, target, write, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    private HTTPResponse listPendingAccessRequests(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID, String accessToken) throws IOException {
        try (CloseableHttpResponse response = IAMClient.listPendingAccessRequests(httpClient, cookie, triplestoreID, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    private HTTPResponse requestAccess(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID, String issuer, boolean write) throws IOException, URISyntaxException {
        try (CloseableHttpResponse response = IAMClient.requestAccess(httpClient, cookie, triplestoreID, issuer, write)) {
            return new HTTPResponse(response);
        }
    }

    private HTTPResponse processAccessRequest(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID, String requestID, boolean accept, String accessToken) throws IOException, URISyntaxException {
        try (CloseableHttpResponse response = IAMClient.processAccessRequest(httpClient, cookie, triplestoreID, requestID, accept, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    private HTTPResponse listUsersWithAccess(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID, boolean write, String accessToken) throws URISyntaxException, IOException {
        try (CloseableHttpResponse response = IAMClient.listUsersWithAccess(httpClient, cookie, triplestoreID, write, accessToken)) {
            return new HTTPResponse(response);
        }
    }

}
