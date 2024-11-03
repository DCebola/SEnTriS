package pt.fct.nova.id.srv.presentation.controllers;


import org.apache.commons.codec.binary.Base64;
import pt.fct.nova.id.srv.application.IAMStorage;
import pt.fct.nova.id.srv.application.clients.LocksClient;
import pt.fct.nova.id.srv.application.crypto.PasswordUtils;
import pt.fct.nova.id.srv.presentation.apis.ControlAPI;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;


/**
 * Resource providing endpoints with control information.
 */
@Path("/ctrl")
public class ControlController implements ControlAPI {
    public Response version() {
        return Response.ok(System.getenv("VERSION")).build();
    }

    public Response init() {
        if (!IAMStorage.isInit()) {
            try {
                System.out.println("Initializing IAM Provider.");
                String defaultAdminUsername = System.getenv("DEFAULT_ADMIN_USERNAME");
                String defaultAdminPassword = Base64.encodeBase64URLSafeString(PasswordUtils.hash(System.getenv("DEFAULT_ADMIN_PASSWORD")));
                String lockID = LocksClient.acquireUserLock(defaultAdminUsername);
                IAMStorage.init(defaultAdminUsername, defaultAdminPassword);
                LocksClient.releaseUserLock(defaultAdminUsername, lockID);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return Response.ok("Service is initialized.").build();
    }
}
