package pt.fct.nova.id.srv.presentation.controllers;

import pt.fct.nova.id.srv.presentation.api.ControlAPI;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * Resource providing endpoints with control information.
 */
@Path("/ctrl")
public class ControlController implements ControlAPI {

    public Response version() {
        return Response.ok(System.getProperty("deployment.version")).build();
    }
}
