package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import pt.fct.nova.id.srv.application.clients.HTTPClient;
import pt.fct.nova.id.srv.application.clients.HTTPUtils;
import pt.fct.nova.id.srv.application.clients.IAMClient;
import pt.fct.nova.id.srv.application.clients.ProxyClient;
import pt.fct.nova.id.srv.application.storage.redis.RedisEncryptedStorageEngine;
import pt.fct.nova.id.srv.presentation.api.EncryptedTriplestoreAPI;

import java.util.*;

import static jakarta.ws.rs.core.Response.Status.*;
import static pt.fct.nova.id.srv.application.clients.HTTPUtils.extractAccessToken;
import static pt.fct.nova.id.srv.presentation.controllers.TriplestoreController.*;


@Path("/encrypted")
public class EncryptedTriplestoreController implements EncryptedTriplestoreAPI {
    private static final String SUCCESS_DELETE_BATCH = "Successful deletion of values from store.";
    private static final RedisEncryptedStorageEngine storageEngine = new RedisEncryptedStorageEngine();

    @Override
    public Response upload(String triplestoreID, Map<String, String> encryptedNodes, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.hasWriteAccess(httpClient, triplestoreID, accessToken)) {
            if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                return HTTPUtils.buildResponse(response);
            storageEngine.save(triplestoreID, encryptedNodes);
            return Response.ok(SUCCESSFUL_UPLOAD).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response prepareSPARQLSearch(String triplestoreID, List<String> trapdoors, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            try (CloseableHttpResponse response = IAMClient.hasReadAccess(httpClient, triplestoreID, accessToken)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HTTPUtils.buildResponse(response);
            }
            try (CloseableHttpResponse response = ProxyClient.saveBindings(httpClient, storageEngine.search(triplestoreID, trapdoors), accessToken)) {
                return HTTPUtils.buildResponse(response);
            }
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response search(String triplestoreID, List<String> trapdoors, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.hasReadAccess(httpClient, triplestoreID, accessToken)) {
            if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                return HTTPUtils.buildResponse(response);

            return Response.ok(storageEngine.search(triplestoreID, trapdoors)).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response delete(String triplestoreID, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.hasOwnerAccess(httpClient, triplestoreID, accessToken)) {
            if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                return HTTPUtils.buildResponse(response);
            storageEngine.delete(triplestoreID);
            return Response.ok(SUCCESS_DELETE_BATCH).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response delete(String triplestoreID, List<String> trapdoors, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.hasWriteAccess(httpClient, triplestoreID, accessToken)) {
            if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                return HTTPUtils.buildResponse(response);
            storageEngine.delete(triplestoreID, trapdoors);
            return Response.ok(SUCCESSFUL_DELETION).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
