package pt.fct.nova.id.srv.presentation.controllers;

import pt.fct.nova.id.srv.presentation.api.StorageAPI;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("storage")
public class StorageController implements StorageAPI {
    @Override
    public Response upload() {
        return Response.ok("NOT IMPLEMENTED").status(Response.Status.NOT_IMPLEMENTED).build();
    }
}
