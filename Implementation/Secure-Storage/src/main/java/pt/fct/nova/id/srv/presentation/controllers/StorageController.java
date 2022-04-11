package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import pt.fct.nova.id.srv.presentation.api.StorageAPI;


@Path("storage")
public class StorageController implements StorageAPI {
    public Response upload() {
        return Response.ok("NOT IMPLEMENTED").status(Response.Status.NOT_IMPLEMENTED).build();
    }
}
