package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.methods.CloseableHttpResponse;
import pt.fct.nova.id.srv.application.clients.HTTPUtils;
import pt.fct.nova.id.srv.application.clients.IAMClient;
import pt.fct.nova.id.srv.presentation.api.UsersAPI;
import pt.fct.nova.id.srv.presentation.api.dtos.AuthForm;
import pt.fct.nova.id.srv.presentation.api.dtos.RequestDecisionForm;

import java.io.IOException;

import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static pt.fct.nova.id.srv.presentation.controllers.ParsingUtils.INTERNAL_ERROR;

@Path("users")
public class UsersController implements UsersAPI {
    @Override
    public Response auth(AuthForm credentialsForm) {
        try {
            return HTTPUtils.buildResponse(IAMClient.authenticate(credentialsForm));
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response registerUser(AuthForm credentialsForm) {
        try {
            return HTTPUtils.buildResponse(IAMClient.registerUser(credentialsForm));
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response deleteUser(Cookie cookie, String username) {
        try {
            return HTTPUtils.buildResponse(IAMClient.deleteUser(cookie, username));
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response issueUpgradeRequest(Cookie cookie, String username) {
        try {
            return HTTPUtils.buildResponse(IAMClient.issueUpgradeRequest(cookie, username));
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response issueDowngradeRequest(Cookie cookie, String username) {
        try {
            return HTTPUtils.buildResponse(IAMClient.issueDowngradeRequest(cookie, username));
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response listPendingRequests(Cookie cookie, String username) {
        try {
            return HTTPUtils.buildResponse(IAMClient.listPendingRoleRequests(cookie, username));
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response processPendingRequest(Cookie cookie, String username, String requestID, RequestDecisionForm decisionForm) {
        try {
            return HTTPUtils.buildResponse(IAMClient.processRoleRequest(cookie, username, requestID, decisionForm));
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }
}
