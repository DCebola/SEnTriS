package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import pt.fct.nova.id.srv.application.SyntaxChecker;
import pt.fct.nova.id.srv.presentation.api.StorageAPI;

import java.io.InputStream;


@Path("storage")
public class StorageController implements StorageAPI {
    private static final String INVALID_SYNTAX_MSG = "Invalid syntax: %s";
    private static final String PARSING_ERROR_MSG = "Error while parsing the file contents.";

    @Override
    public Response upload(String id, String syntax, InputStream contents) {
        Model model = ModelFactory.createDefaultModel();
        if (SyntaxChecker.check(syntax)) {
            try {
                model.read(contents, syntax);
                //TODO: Store Indexes
                //TODO: Store Triples
                return Response.ok("NOT IMPLEMENTED").status(Response.Status.NOT_IMPLEMENTED).build();
            } catch (Exception e) {
                return Response.ok(PARSING_ERROR_MSG).status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } else
            return Response.ok(String.format(INVALID_SYNTAX_MSG, syntax)).status(Response.Status.BAD_REQUEST).build();
    }

    @Override
    public Response query(String protocol) {
        //TODO: Check protocol type (e.g SPARQL, ...)
        //TODO: Parse query body
        //TODO: Collect results to send into correct type (boolean, graph or binding tables)
        return Response.ok("NOT IMPLEMENTED").status(Response.Status.NOT_IMPLEMENTED).build();
    }
}
