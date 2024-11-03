package pt.fct.nova.id.srv.presentation.controllers;


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
}
