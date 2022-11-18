package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.lang.CollectorStreamTriples;
import pt.fct.nova.id.srv.application.query.SPARQLQueryEngine;
import pt.fct.nova.id.srv.application.storage.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.storage.exceptions.StoreAlreadyExistsException;
import pt.fct.nova.id.srv.application.storage.exceptions.StoreNotFoundException;
import pt.fct.nova.id.srv.application.storage.redis.RStorageEngine;
import pt.fct.nova.id.srv.application.triplestores.TriplestoreImpl;
import pt.fct.nova.id.srv.application.triplestores.Triplestore;
import pt.fct.nova.id.srv.presentation.api.TriplestoreAPI;
import pt.fct.nova.id.srv.presentation.api.dtos.UploadForm;
import pt.fct.nova.id.srv.presentation.exceptions.UnknownRDFLanguageException;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;


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
    private static final String SUCCESS_DELETE = "Store %s deleted.";

    private final Triplestore triplestore = new TriplestoreImpl(new RStorageEngine(), new SPARQLQueryEngine());

    @Override
    public Response create(String storeID, UploadForm form) {
        try {

            triplestore.createDataset(
                    storeID,
                    parseTriples(form.getContents(), parseRDFLanguage(form.getSyntax())),
                    form.getNamespaces()
            );
            return Response.ok(SUCCESS_UPLOAD).build();
        } catch (InvalidNodeException e) {
            return Response.ok(BAD_NODE).status(Status.BAD_REQUEST).build();
        } catch (StoreAlreadyExistsException e) {
            return Response.ok(String.format(STORE_ALREADY_EXISTS, storeID)).status(Status.BAD_REQUEST).build();
        } catch (UnknownRDFLanguageException e) {
            return Response.ok(String.format(INVALID_SYNTAX_MSG, form.getSyntax())).status(Status.BAD_REQUEST).build();
        } catch (Exception e) {
            return Response.ok(PARSING_ERROR_MSG).status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Lang parseRDFLanguage(String syntax) throws UnknownRDFLanguageException {
        Lang l = RDFLanguages.nameToLang(syntax);
        if (l == null)
            throw new UnknownRDFLanguageException();
        return l;
    }

    private List<Triple> parseTriples(InputStream content, Lang lang) {
        CollectorStreamTriples tripleCollector = new CollectorStreamTriples();
        RDFParser.source(content).lang(lang).parse(tripleCollector);
        return tripleCollector.getCollected();
    }

    @Override
    public Response upload(String storeID, UploadForm form) {
        try {
            triplestore.uploadData(
                    storeID,
                    parseTriples(form.getContents(), parseRDFLanguage(form.getSyntax())),
                    form.getNamespaces()
            );
            return Response.ok(SUCCESS_UPLOAD).build();
        } catch (InvalidNodeException e) {
            return Response.ok(BAD_NODE).status(Status.BAD_REQUEST).build();
        } catch (StoreNotFoundException e) {
            return Response.ok(String.format(STORE_NOT_FOUND, storeID)).status(Status.NOT_FOUND).build();
        } catch (UnknownRDFLanguageException e) {
            return Response.ok(String.format(INVALID_SYNTAX_MSG, form.getSyntax())).status(Status.BAD_REQUEST).build();
        } catch (Exception e) {
            return Response.ok(PARSING_ERROR_MSG).status(Status.INTERNAL_SERVER_ERROR).build();
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
            return Response.ok(QUERY_ERROR_MSG).status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response delete(String storeID) {
        try {
            triplestore.delete(storeID);
            return Response.ok(String.format(SUCCESS_DELETE, storeID)).build();
        } catch (StoreNotFoundException e) {
            return Response.ok(String.format(STORE_NOT_FOUND, storeID)).status(Status.NOT_FOUND).build();
        }
    }

}
