package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.system.AsyncParser;
import pt.fct.nova.id.srv.application.triplestores.Triplestore;
import pt.fct.nova.id.srv.presentation.api.StorageAPI;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;


@Path("storage")
public class StorageController implements StorageAPI {
    private static final String INVALID_SYNTAX_MSG = "Invalid syntax: %s";
    private static final String PARSING_ERROR_MSG = "Error while parsing the file contents.";
    private static final String WRITING_ERROR_MSG = "Error while downloading the dataset.";

    private final Triplestore triplestore = new Triplestore();

    @Override
    public Response upload(String storeID, String syntax, InputStream contents) {
        Lang l = RDFLanguages.nameToLang(syntax);
        if (l == null)
            return Response.ok(String.format(INVALID_SYNTAX_MSG, syntax)).status(Status.BAD_REQUEST).build();
        try {
            triplestore.createDataset(storeID, AsyncParser.asyncParseTriples(contents, l, null));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            RDFDataMgr.writeTriples(out, triplestore.getDataset(storeID));
            out.toByteArray();

            return Response.ok("NOT IMPLEMENTED").status(Status.NOT_IMPLEMENTED).build();
        } catch (Exception e) {
            return Response.ok(PARSING_ERROR_MSG).status(Status.INTERNAL_SERVER_ERROR).build();
        }

    }

    @Override
    public Response download(String storeID, String syntax) {
        Lang l = RDFLanguages.nameToLang(syntax);
        if (l == null)
            return Response.ok(String.format(INVALID_SYNTAX_MSG, syntax)).status(Status.BAD_REQUEST).build();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            RDFDataMgr.writeTriples(out, triplestore.getDataset(storeID));
            //return Response.ok(out.toByteArray()).build();
            return Response.ok(out.toByteArray()).status(Status.NOT_IMPLEMENTED).build();
        } catch (Exception e) {
            return Response.ok(WRITING_ERROR_MSG).status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response query(String storeID) {
        //TODO: Check protocol type (e.g SPARQL, ...)
        //TODO: Parse query body
        //TODO: Collect results to send into correct type (boolean, graph or binding tables)
        return Response.ok("NOT IMPLEMENTED").status(Status.NOT_IMPLEMENTED).build();
    }

}
