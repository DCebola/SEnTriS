package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import pt.fct.nova.id.srv.application.clients.HTTPClient;
import pt.fct.nova.id.srv.application.clients.HTTPUtils;
import pt.fct.nova.id.srv.application.clients.IAMClient;
import pt.fct.nova.id.srv.application.query.execution.SecureSPARQLWorker;
import pt.fct.nova.id.srv.application.query.execution.SimpleSPARQLExecution;
import pt.fct.nova.id.srv.application.query.plans.QueryExecutionPlan;
import pt.fct.nova.id.srv.application.storage.EncryptedStorageEngine;
import pt.fct.nova.id.srv.application.storage.redis.EncryptedRStorageEngine;
import pt.fct.nova.id.srv.presentation.api.EncryptedTriplestoreAPI;

import java.io.ByteArrayOutputStream;
import java.util.*;

import static jakarta.ws.rs.core.Response.Status.*;
import static pt.fct.nova.id.srv.application.clients.HTTPUtils.extractAccessToken;
import static pt.fct.nova.id.srv.presentation.controllers.TriplestoreController.*;
import static pt.fct.nova.id.srv.presentation.controllers.TriplestoreController.NOT_IMPLEMENTED_ERROR;


@Path("secure")
public class EncryptedTriplestoreController implements EncryptedTriplestoreAPI {
    private static final String SUCCESS_DELETE_BATCH = "Successful deletion of values from store.";
    EncryptedStorageEngine storageEngine = new EncryptedRStorageEngine();

    @Override
    public Response upload(Cookie cookie, String storeID, Map<String, String> encryptedNodes, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.hasWriteAccess(httpClient, cookie, storeID, accessToken)) {
            if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                return HTTPUtils.buildResponse(response);
            storageEngine.save(storeID, encryptedNodes);
            return Response.ok(SUCCESSFUL_UPLOAD).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response answerSPARQLQuery(Cookie cookie, String storeID, QueryExecutionPlan queryExecutionPlan, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.hasReadAccess(httpClient, cookie, storeID, accessToken)) {
            if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                return HTTPUtils.buildResponse(response);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ResultSet res = new SimpleSPARQLExecution(queryExecutionPlan).exec(new SecureSPARQLWorker(storeID, storageEngine));
            ResultSetFormatter.outputAsJSON(out, res);
            return Response.ok(out.toByteArray()).build();
        } catch (NotImplemented e) {
            return Response.ok(NOT_IMPLEMENTED_ERROR).status(NOT_IMPLEMENTED).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response search(Cookie cookie, String storeID, List<String> trapdoors, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.hasReadAccess(httpClient, cookie, storeID, accessToken)) {
            if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                return HTTPUtils.buildResponse(response);

            return Response.ok(storageEngine.search(storeID, trapdoors)).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response delete(Cookie cookie, String storeID, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.hasOwnerAccess(httpClient, cookie, storeID, accessToken)) {
            if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                return HTTPUtils.buildResponse(response);
            storageEngine.delete(storeID);
            return Response.ok(SUCCESS_DELETE_BATCH).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response delete(Cookie cookie, String storeID, List<String> trapdoors, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.hasWriteAccess(httpClient, cookie, storeID, accessToken)) {
            if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                return HTTPUtils.buildResponse(response);
            storageEngine.delete(storeID, trapdoors);
            return Response.ok(SUCCESSFUL_DELETION).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
