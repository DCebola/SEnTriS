package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ParseException;
import pt.fct.nova.id.srv.application.clients.HTTPClient;
import pt.fct.nova.id.srv.application.clients.HTTPResponse;
import pt.fct.nova.id.srv.application.clients.HTTPUtils;
import pt.fct.nova.id.srv.application.clients.IAMClient;
import pt.fct.nova.id.srv.presentation.apis.UsersAPI;
import pt.fct.nova.id.srv.presentation.dtos.AuthForm;
import pt.fct.nova.id.srv.presentation.dtos.RequestDecisionForm;

import java.io.IOException;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

@Path("users")
public class UsersController implements UsersAPI {
    @Override
    public Response auth(AuthForm credentialsForm) {
        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.authenticate(httpClient, credentialsForm)) {
            NewCookie cookie = HTTPUtils.extractCookie(response);
            return new HTTPResponse(cookie, response).build();
        } catch (IOException | ParseException e) {
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response registerUser(AuthForm credentialsForm) {
        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.registerUser(httpClient, credentialsForm)) {
            return new HTTPResponse(response).build();
        } catch (IOException | ParseException e) {
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response deleteUser(Cookie cookie, String username) {
        if(cookie == null)
            return Response.status(BAD_REQUEST).build();
        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.deleteUser(httpClient, cookie, username)) {
            return new HTTPResponse(response).build();
        } catch (IOException | ParseException e) {
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response issueUpgradeRequest(Cookie cookie, String username) {
        if(cookie == null)
            return Response.status(BAD_REQUEST).build();
        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.issueUpgradeRequest(httpClient, cookie, username)) {
            return new HTTPResponse(response).build();
        } catch (IOException | ParseException e) {
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response issueDowngradeRequest(Cookie cookie, String username) {
        if(cookie == null)
            return Response.status(BAD_REQUEST).build();
        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.issueDowngradeRequest(httpClient, cookie, username)) {
            return new HTTPResponse(response).build();
        } catch (IOException | ParseException e) {
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response listPendingRequests(Cookie cookie, String username) {
        if(cookie == null)
            return Response.status(BAD_REQUEST).build();
        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.listPendingRoleRequests(httpClient, cookie, username)) {
            return new HTTPResponse(response).build();
        } catch (IOException | ParseException e) {
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response processPendingRequest(Cookie cookie, String username, String requestID, RequestDecisionForm decisionForm) {
        if(cookie == null)
            return Response.status(BAD_REQUEST).build();
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            try (CloseableHttpResponse response = IAMClient.processRoleRequest(httpClient, cookie, username, requestID, decisionForm)) {
                return new HTTPResponse(response).build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }
    }
}
