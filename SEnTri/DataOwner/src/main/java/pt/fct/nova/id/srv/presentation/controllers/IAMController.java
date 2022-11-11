package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import pt.fct.nova.id.srv.presentation.api.IdentityAndAccessManagementAPI;
import pt.fct.nova.id.srv.presentation.api.dtos.AuthForm;
import pt.fct.nova.id.srv.presentation.api.dtos.IAMRequestForm;

@Path("/iam")
public class IAMController implements IdentityAndAccessManagementAPI {
    @Override
    public Response register(AuthForm credentials) {
        return null;
    }

    @Override
    public Response delete(String username) {
        return null;
    }

    @Override
    public Response auth(AuthForm credentials) {
        return null;
    }

    @Override
    public Response listPendingIAMRequests(Cookie cookie, IAMRequestForm request) {
        return null;
    }

    @Override
    public Response processIAMRequest(Cookie cookie, IAMRequestForm request) {
        return null;
    }

    @Override
    public Response issueIAMRequest(Cookie cookie, IAMRequestForm request) {
        return null;
    }
}
