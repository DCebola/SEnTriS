package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import pt.fct.nova.id.srv.presentation.api.IdentityAndAccessManagementAPI;
import pt.fct.nova.id.srv.presentation.api.dtos.AccessPolicyForm;
import pt.fct.nova.id.srv.presentation.api.dtos.AuthForm;
import pt.fct.nova.id.srv.presentation.api.dtos.UserDTO;

@Path("/iam")
public class IAMController implements IdentityAndAccessManagementAPI {

    @Override
    public Response auth(AuthForm credentials) {
        return null;
    }

    @Override
    public Response addUser(AuthForm credentials) {
        return null;
    }

    @Override
    public Response updateUser(Cookie cookie, String username, UserDTO user) {
        return null;
    }

    @Override
    public Response deleteUSer(Cookie cookie, String username) {
        return null;
    }

    @Override
    public Response issueGrantAccessRequest(Cookie cookie, String username, AccessPolicyForm accessPolicy) {
        return null;
    }

    @Override
    public Response revokeAccess(Cookie cookie, String username, AccessPolicyForm accessPolicy) {
        return null;
    }

    @Override
    public Response getPendingRoleRequests(Cookie cookie) {
        return null;
    }

    @Override
    public Response getStoreAccessPolicy(Cookie cookie, String storeID) {
        return null;
    }

    @Override
    public Response createStoreAccessPolicy(Cookie cookie, String storeID, AccessPolicyForm accessPolicy) {
        return null;
    }

    @Override
    public Response grantAccess(Cookie cookie, String storeID, AccessPolicyForm accessPolicy) {
        return null;
    }

    @Override
    public Response deleteStoreAccessPolicy(Cookie cookie, String storeID) {
        return null;
    }
}
