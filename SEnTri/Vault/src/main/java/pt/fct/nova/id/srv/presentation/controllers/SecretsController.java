package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import pt.fct.nova.id.srv.application.storage.redis.VaultStorage;
import pt.fct.nova.id.srv.application.clients.HTTPClient;
import pt.fct.nova.id.srv.application.clients.HTTPUtils;
import pt.fct.nova.id.srv.application.clients.IAMClient;
import pt.fct.nova.id.srv.presentation.apis.SecretsAPI;

import java.io.*;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static jakarta.ws.rs.core.Response.Status.*;
import static pt.fct.nova.id.srv.application.clients.HTTPUtils.extractAccessToken;

@Path("/secrets")
public class SecretsController implements SecretsAPI {
    private static final String INTERNAL_ERROR = "Internal error.";
    private static final String SECRETS_ALREADY_EXIST = "Triplestore secrets already exists.";
    private static final String SUCCESSFUL_SECRETS_CREATION = "Successful secrets creation.";
    private static final String SUCCESSFUL_SECRETS_DELETION = "Successful secrets deletion.";
    private static final String UNKNOWN_SECRETS = "Triplestore secrets not found.";
    private static final String NO_ACCESS_TOKEN = "Malformed request: bearer token required.";

    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder();

    @Override
    public Response createSecrets(String triplestoreID, byte[] secrets, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();
        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.hasOwnerAccess(httpClient, triplestoreID, accessToken);
             ByteArrayInputStream is = new ByteArrayInputStream(secrets);
             ObjectInputStream ois = new ObjectInputStream(is)) {
            if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                return HTTPUtils.buildResponse(response);
            if (VaultStorage.exists(triplestoreID))
                return Response.ok(SECRETS_ALREADY_EXIST).status(NOT_FOUND).build();
            VaultStorage.saveSecrets(triplestoreID, (Map<String, String>) ois.readObject());
            return Response.ok(SUCCESSFUL_SECRETS_CREATION).build();
        } catch (IOException | ClassNotFoundException e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response getSecrets(String triplestoreID, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.hasReadAccess(httpClient, triplestoreID, accessToken);
             ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {

            if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                return HTTPUtils.buildResponse(response);
            if (!VaultStorage.exists(triplestoreID))
                return Response.ok(UNKNOWN_SECRETS).status(NOT_FOUND).build();
            oos.writeObject(VaultStorage.getSecrets(triplestoreID));
            return Response.ok(base64Encoder.encodeToString(bos.toByteArray())).build();
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response deleteSecrets(String triplestoreID, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.hasOwnerAccess(httpClient, triplestoreID, accessToken)) {
            if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                return HTTPUtils.buildResponse(response);
            if (!VaultStorage.exists(triplestoreID))
                return Response.ok(UNKNOWN_SECRETS).status(NOT_FOUND).build();
            VaultStorage.deleteSecrets(triplestoreID);
            return Response.ok(SUCCESSFUL_SECRETS_DELETION).build();
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

}
