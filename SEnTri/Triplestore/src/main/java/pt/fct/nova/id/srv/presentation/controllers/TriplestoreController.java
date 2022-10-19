package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.system.AsyncParser;
import pt.fct.nova.id.srv.application.query.QueryEngine;
import pt.fct.nova.id.srv.application.query.SPARQLQueryEngine;
import pt.fct.nova.id.srv.application.query.execution.exceptions.SPARQLExecutionException;
import pt.fct.nova.id.srv.application.storage.StorageEngine;
import pt.fct.nova.id.srv.application.storage.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.storage.exceptions.StoreAlreadyExistsException;
import pt.fct.nova.id.srv.application.storage.exceptions.StoreNotFoundException;
import pt.fct.nova.id.srv.application.storage.redis.RStorageEngine;
import pt.fct.nova.id.srv.application.triplestores.SimpleTriplestore;
import pt.fct.nova.id.srv.application.triplestores.Triplestore;
import pt.fct.nova.id.srv.presentation.api.TriplestoreAPI;
import pt.fct.nova.id.srv.presentation.api.dtos.UploadForm;

import java.io.ByteArrayOutputStream;


@Path("triplestore")
public class TriplestoreController implements TriplestoreAPI {
    private static final String INVALID_SYNTAX_MSG = "Invalid syntax: %s";
    private static final String PARSING_ERROR_MSG = "Error while parsing the file contents.";
    private static final String DOWNLOAD_ERROR_MSG = "Error while downloading the dataset.";
    private static final String SUCCESS_UPLOAD = "Successful upload.";
    private static final String NOT_IMPLEMENTED = "Operation not yet supported.";
    private static final String QUERY_ERROR_MSG = "Error while executing query.";
    private static final String STORE_ALREADY_EXISTS = "Store %s already exists.";
    private static final String STORE_NOT_FOUND = "Store %s not found.";
    private static final String BAD_NODE = "Data must only contain concrete nodes: IRI, Blank, Literal.";

    private final Triplestore triplestore = generateTriplestore(System.getenv("STORAGE_ENGINE"), System.getenv("QUERY_ENGINE"));

    private Triplestore generateTriplestore(String storageEngineType, String queryEngineType) {
        try {
            return new SimpleTriplestore(
                    (StorageEngine) Class.forName(storageEngineType).getConstructor().newInstance(),
                    (QueryEngine) Class.forName(queryEngineType).getConstructor().newInstance());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new SimpleTriplestore(new RStorageEngine(), new SPARQLQueryEngine());
    }

    @Override
    public Response create(String storeID, UploadForm form) {
        Lang l = RDFLanguages.nameToLang(form.getSyntax());
        if (l == null)
            return Response.ok(String.format(INVALID_SYNTAX_MSG, form.getSyntax())).status(Status.BAD_REQUEST).build();
        else {
            try {
                triplestore.createDataset(
                        storeID,
                        AsyncParser.asyncParseTriples(form.getContents(), l, null),
                        form.getNamespaces()
                );
                System.out.println("Created database.");
                return Response.ok(SUCCESS_UPLOAD).build();
            } catch (InvalidNodeException e) {
                return Response.ok(BAD_NODE).status(Status.BAD_REQUEST).build();
            } catch (StoreAlreadyExistsException e) {
                return Response.ok(String.format(STORE_ALREADY_EXISTS, storeID)).status(Status.BAD_REQUEST).build();
            } catch (Exception e) {
                return Response.ok(PARSING_ERROR_MSG).status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    @Override
    public Response upload(String storeID, UploadForm form) {
        Lang l = RDFLanguages.nameToLang(form.getSyntax());
        if (l == null)
            return Response.ok(String.format(INVALID_SYNTAX_MSG, form.getSyntax())).status(Status.BAD_REQUEST).build();
        else {
            try {
                triplestore.uploadData(
                        storeID,
                        AsyncParser.asyncParseTriples(form.getContents(), l, null),
                        form.getNamespaces()
                );
                return Response.ok(SUCCESS_UPLOAD).build();
            } catch (InvalidNodeException e) {
                return Response.ok(BAD_NODE).status(Status.BAD_REQUEST).build();
            } catch (StoreNotFoundException e) {
                return Response.ok(String.format(STORE_NOT_FOUND, storeID)).status(Status.NOT_FOUND).build();
            } catch (Exception e) {
                return Response.ok(PARSING_ERROR_MSG).status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    @Override
    public Response download(String storeID, String syntax) {
        Lang l = RDFLanguages.nameToLang(syntax);
        if (l == null)
            return Response.ok(String.format(INVALID_SYNTAX_MSG, syntax)).status(Status.BAD_REQUEST).build();
        try {
            Model m = triplestore.getDatasetModel(storeID);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            RDFDataMgr.write(out, m, l);
            return Response.ok(out.toByteArray()).build();
        } catch (StoreNotFoundException e) {
            return Response.ok(String.format(STORE_NOT_FOUND, storeID)).status(Status.NOT_FOUND).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok(DOWNLOAD_ERROR_MSG).status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    public Response answerSPARQLQuery(String storeID, String query) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ResultSet res = triplestore.executeQuery(storeID, query);
            ResultSetFormatter.outputAsJSON(out, res);
            return Response.ok(out.toByteArray()).build();
        } catch (StoreNotFoundException e) {
            return Response.ok(String.format(STORE_NOT_FOUND, storeID)).status(Status.NOT_FOUND).build();
        } catch (NotImplemented e) {
            return Response.ok(NOT_IMPLEMENTED).status(Status.NOT_IMPLEMENTED).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok(QUERY_ERROR_MSG).status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

}
