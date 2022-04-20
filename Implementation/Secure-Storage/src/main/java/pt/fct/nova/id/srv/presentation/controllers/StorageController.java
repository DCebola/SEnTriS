package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFParser;
import pt.fct.nova.id.srv.presentation.api.StorageAPI;

import java.io.ByteArrayInputStream;


@Path("storage")
public class StorageController implements StorageAPI {

    @Override
    public Response upload() {
        Model model = ModelFactory.createDefaultModel();
        model.read(new ByteArrayInputStream(new byte[10]), "lang");
        for (Triple t : model.getGraph().stream().toList()) {


        }
        //TODO: Store Indexes
        //TODO: Store Triples
        return Response.ok("NOT IMPLEMENTED").status(Response.Status.NOT_IMPLEMENTED).build();
    }

    @Override
    public Response query(String protocol, String body) {
        //TODO: Check protocol type (e.g SPARQL, ...)
        //TODO: Parse query body
        //TODO: Collect results to send into correct type (boolean, graph or binding tables)
        return Response.ok("NOT IMPLEMENTED").status(Response.Status.NOT_IMPLEMENTED).build();
    }
}
