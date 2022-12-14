package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import pt.fct.nova.id.srv.application.Vault;
import pt.fct.nova.id.srv.application.clients.HTTPClient;
import pt.fct.nova.id.srv.application.clients.HTTPUtils;
import pt.fct.nova.id.srv.application.clients.IAMClient;
import pt.fct.nova.id.srv.presentation.api.SecretsAPI;
import pt.fct.nova.id.srv.presentation.api.dtos.SecretsForm;

import java.io.IOException;
import java.util.List;

import static jakarta.ws.rs.core.Response.Status.*;
import static pt.fct.nova.id.srv.application.clients.HTTPUtils.extractAccessToken;

@Path("secrets")
public class SecretsController implements SecretsAPI {
    private static final String INTERNAL_ERROR = "Internal error.";
    private static final String SECRETS_ALREADY_EXIST = "Triplestore secrets already exists.";
    private static final String SUCCESSFUL_SECRETS_CREATION = "Successful secrets creation.";
    private static final String SUCCESSFUL_SECRETS_DELETION = "Successful secrets deletion.";
    private static final String UNKNOWN_SECRETS = "Triplestore secrets not found.";
    private static final String NO_ACCESS_TOKEN = "Malformed request: bearer token required.";

    @Override
    public Response createSecrets(Cookie cookie, SecretsForm form, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();
        String triplestoreID = form.getTriplestoreID();

        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.hasOwnerAccess(httpClient, cookie, triplestoreID, accessToken)) {
            if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                return HTTPUtils.buildResponse(response);
            if (Vault.exists(triplestoreID))
                return Response.ok(SECRETS_ALREADY_EXIST).status(NOT_FOUND).build();
            Vault.saveSecrets(triplestoreID, form.getSecrets());
            return Response.ok(SUCCESSFUL_SECRETS_CREATION).build();
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response getSecrets(Cookie cookie, String triplestoreID, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.hasReadAccess(httpClient, cookie, triplestoreID, accessToken)) {
            if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                return HTTPUtils.buildResponse(response);
            if (!Vault.exists(triplestoreID))
                return Response.ok(UNKNOWN_SECRETS).status(NOT_FOUND).build();
            return Response.ok(Vault.getSecrets(triplestoreID)).build();
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response deleteSecrets(Cookie cookie, String triplestoreID, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.hasOwnerAccess(httpClient, cookie, triplestoreID, accessToken)) {
            if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                return HTTPUtils.buildResponse(response);
            if (!Vault.exists(triplestoreID))
                return Response.ok(UNKNOWN_SECRETS).status(NOT_FOUND).build();
            Vault.deleteSecrets(triplestoreID);
            return Response.ok(SUCCESSFUL_SECRETS_DELETION).build();
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

}
