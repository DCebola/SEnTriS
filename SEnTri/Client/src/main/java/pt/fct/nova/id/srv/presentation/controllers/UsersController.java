package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.methods.CloseableHttpResponse;
import pt.fct.nova.id.srv.application.clients.HttpUtils;
import pt.fct.nova.id.srv.application.clients.IAMClient;
import pt.fct.nova.id.srv.presentation.api.UsersAPI;
import pt.fct.nova.id.srv.presentation.api.dtos.AuthForm;

import java.io.IOException;

import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static pt.fct.nova.id.srv.presentation.controllers.ClientUtils.INTERNAL_ERROR;

@Path("users")
public class UsersController implements UsersAPI {
    @Override
    public Response auth(AuthForm credentialsForm) {
        try (CloseableHttpResponse response = IAMClient.authenticate(credentialsForm)) {
            return HttpUtils.buildResponse(response);
        }  catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response registerUser(AuthForm credentialsForm) {
        try (CloseableHttpResponse response = IAMClient.registerUser(credentialsForm)) {
            return HttpUtils.buildResponse(response);
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();        }
    }

    @Override
    public Response deleteUser(Cookie cookie, String username) {
        try (CloseableHttpResponse response = IAMClient.deleteUser(cookie, username)) {
            return HttpUtils.buildResponse(response);
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response issueUpgradeRequest(Cookie cookie, String username) {
        try (CloseableHttpResponse response = IAMClient.issueUpgradeRequest(cookie, username)) {
            return HttpUtils.buildResponse(response);
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }
}
