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
import pt.fct.nova.id.srv.application.query.execution.SimpleSPARQLExecution;
import pt.fct.nova.id.srv.application.query.execution.SimpleSPARQLWorker;
import pt.fct.nova.id.srv.application.query.plans.QueryExecutionPlan;
import pt.fct.nova.id.srv.application.storage.StorageEngine;
import pt.fct.nova.id.srv.application.storage.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.storage.redis.RStorageEngine;
import pt.fct.nova.id.srv.presentation.api.TriplestoreAPI;
import pt.fct.nova.id.srv.presentation.api.dtos.UploadForm;
import pt.fct.nova.id.srv.presentation.exceptions.UnknownRDFLanguageException;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;


public class TriplestoreController implements TriplestoreAPI {
    private static final String INVALID_SYNTAX_MSG = "Invalid syntax.";
    public static final String UPLOAD_ERROR = "Error while uploading content.";
    public static final String SUCCESSFUL_UPLOAD = "Successful upload.";
    public static final String SUCCESSFUL_DELETION = "Store deleted.";
    public static final String NOT_IMPLEMENTED = "Operation not yet supported.";
    public static final String QUERY_ERROR = "Error while executing query.";
    private static final String BAD_NODE = "Data must only contain concrete nodes: IRI, Blank, Literal.";


    private final StorageEngine storageEngine = new RStorageEngine();

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
            storageEngine.saveTriples(storeID, parseTriples(form.getContents(), parseRDFLanguage(form.getSyntax())));
            storageEngine.saveNamespaces(storeID, form.getNamespaces());
            return Response.ok(SUCCESSFUL_UPLOAD).build();
        } catch (InvalidNodeException e) {
            return Response.ok(BAD_NODE).status(Status.BAD_REQUEST).build();
        } catch (UnknownRDFLanguageException e) {
            return Response.ok(INVALID_SYNTAX_MSG).status(Status.BAD_REQUEST).build();
        } catch (Exception e) {
            return Response.ok(UPLOAD_ERROR).status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    public Response answerSPARQLQuery(String storeID, QueryExecutionPlan queryExecutionPlan) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ResultSet res = new SimpleSPARQLExecution(queryExecutionPlan).exec(new SimpleSPARQLWorker(storeID, storageEngine));
            ResultSetFormatter.outputAsJSON(out, res);
            return Response.ok(out.toByteArray()).build();
        } catch (NotImplemented e) {
            return Response.ok(NOT_IMPLEMENTED).status(Status.NOT_IMPLEMENTED).build();
        } catch (Exception e) {
            return Response.ok(QUERY_ERROR).status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response delete(String storeID) {
        storageEngine.deleteStore(storeID);
        return Response.ok(SUCCESSFUL_DELETION).build();
    }

}
