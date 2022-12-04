package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import pt.fct.nova.id.srv.application.clients.HttpUtils;
import pt.fct.nova.id.srv.application.clients.IAMClient;
import pt.fct.nova.id.srv.application.query.execution.SimpleSPARQLExecution;
import pt.fct.nova.id.srv.application.query.execution.SimpleSPARQLWorker;
import pt.fct.nova.id.srv.application.query.plans.QueryExecutionPlan;
import pt.fct.nova.id.srv.application.storage.StorageEngine;
import pt.fct.nova.id.srv.application.storage.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.storage.redis.RStorageEngine;
import pt.fct.nova.id.srv.presentation.api.TriplestoreAPI;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static jakarta.ws.rs.core.Response.Status.*;
import static pt.fct.nova.id.srv.application.clients.HttpUtils.extractAccessToken;


public class TriplestoreController implements TriplestoreAPI {
    public static final String NO_ACCESS_TOKEN = "Malformed request: bearer token required.";
    public static final String INTERNAL_ERROR = "Internal error.";
    public static final String SUCCESSFUL_UPLOAD = "Successful upload.";
    public static final String SUCCESSFUL_DELETION = "Store deleted.";
    public static final String NOT_IMPLEMENTED_ERROR = "Operation not yet supported.";
    private static final String BAD_NODE = "Data must only contain concrete nodes: IRI, Blank, Literal.";
    private static final StorageEngine storageEngine = new RStorageEngine();

    @Override
    public Response upload(Cookie cookie, String storeID, List<Triple> triples, List<String> authorizationHeaders) {
        try {
            String accessToken = extractAccessToken(authorizationHeaders);
            if (accessToken == null)
                return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

            try (CloseableHttpResponse response = IAMClient.hasWriteAccess(cookie, storeID, accessToken)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
            }
            storageEngine.saveTriples(storeID, triples);
            return Response.ok(SUCCESSFUL_UPLOAD).build();
        } catch (InvalidNodeException e) {
            return Response.ok(BAD_NODE).status(Status.BAD_REQUEST).build();
        }  catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    public Response answerSPARQLQuery(Cookie cookie, String storeID, QueryExecutionPlan queryExecutionPlan, List<String> authorizationHeaders) {
        try {
            String accessToken = extractAccessToken(authorizationHeaders);
            if (accessToken == null)
                return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

            try (CloseableHttpResponse response = IAMClient.hasReadAccess(cookie, storeID, accessToken)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ResultSet res = new SimpleSPARQLExecution(queryExecutionPlan).exec(new SimpleSPARQLWorker(storeID, storageEngine));
            ResultSetFormatter.outputAsJSON(out, res);
            return Response.ok(out.toByteArray()).build();
        } catch (NotImplemented e) {
            return Response.ok(NOT_IMPLEMENTED_ERROR).status(NOT_IMPLEMENTED).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response delete(Cookie cookie, String storeID, List<String> authorizationHeaders) {
        try {
            String accessToken = extractAccessToken(authorizationHeaders);
            if (accessToken == null)
                return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

            try (CloseableHttpResponse response = IAMClient.hasOwnerAccess(cookie, storeID, accessToken)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
            }
            storageEngine.deleteStore(storeID);
            return Response.ok(SUCCESSFUL_DELETION).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response delete(Cookie cookie, String storeID, List<Triple> triples, List<String> authorizationHeaders) {
        try {
            String accessToken = extractAccessToken(authorizationHeaders);
            if (accessToken == null)
                return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

            try (CloseableHttpResponse response = IAMClient.hasWriteAccess(cookie, storeID, accessToken)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
            }
            return Response.ok(NOT_IMPLEMENTED_ERROR).status(Status.NOT_IMPLEMENTED).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
