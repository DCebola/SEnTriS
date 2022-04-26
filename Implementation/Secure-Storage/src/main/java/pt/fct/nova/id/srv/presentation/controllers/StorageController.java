package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import pt.fct.nova.id.srv.presentation.api.StorageAPI;
import pt.fct.nova.id.srv.presentation.dto.UploadRequestBody;

import java.io.ByteArrayInputStream;


@Path("storage")
public class StorageController implements StorageAPI {

    @Override
    public Response upload(String id, UploadRequestBody body) {
        Model model = ModelFactory.createDefaultModel();

        //TODO: Verify body syntax
        model.read(new ByteArrayInputStream(body.getContents()), body.getSyntax());
        for (Triple t : model.getGraph().stream().toList()) {
            System.out.println("[" + t.getSubject() + "] -> " + "[" + t.getPredicate() + "] -> " + "[" + t.getObject() + "]");
        }
        //TODO: Store Indexes
        //TODO: Store Triples
        return Response.ok("NOT IMPLEMENTED").status(Response.Status.NOT_IMPLEMENTED).build();
    }

    @Override
    public Response query(String protocol) {
        //TODO: Check protocol type (e.g SPARQL, ...)
        //TODO: Parse query body
        //TODO: Collect results to send into correct type (boolean, graph or binding tables)
        return Response.ok("NOT IMPLEMENTED").status(Response.Status.NOT_IMPLEMENTED).build();
    }
}
