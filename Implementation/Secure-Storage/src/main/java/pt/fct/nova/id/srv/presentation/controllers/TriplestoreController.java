package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.system.AsyncParser;
import pt.fct.nova.id.srv.application.query.QueryEngine;
import pt.fct.nova.id.srv.application.query.SPARQLQueryEngine;
import pt.fct.nova.id.srv.application.query.plans.SimpleSPARQLPlanner;
import pt.fct.nova.id.srv.application.storage.StorageEngine;
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
    private static final String WRITING_ERROR_MSG = "Error while downloading the dataset.";
    private static final String SUCCESS_UPLOAD = "Successful upload.";

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
    public Response upload(String storeID, UploadForm form) {
        Lang l = RDFLanguages.nameToLang(form.getSyntax());
        if (l == null)
            return Response.ok(String.format(INVALID_SYNTAX_MSG, form.getSyntax())).status(Status.BAD_REQUEST).build();
        else {
            boolean success = triplestore.createDataset(
                    storeID,
                    AsyncParser.asyncParseTriples(form.getContents(), l, null),
                    form.getNamespaces()
            );
            if (!success)
                return Response.ok(PARSING_ERROR_MSG).status(Status.INTERNAL_SERVER_ERROR).build();
            else {
                return Response.ok(SUCCESS_UPLOAD).build();
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
            if (m == null)
                return Response.ok(WRITING_ERROR_MSG).status(Status.INTERNAL_SERVER_ERROR).build();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            RDFDataMgr.write(out, m, l);

            return Response.ok(out.toByteArray()).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok(WRITING_ERROR_MSG).status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    public Response answerSPARQLQuery(String storeID, String query) {
        try {
            System.out.println(query);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ResultSet res = triplestore.executeQuery(storeID, query);
            ResultSetFormatter.outputAsJSON(out, res);
            return Response.ok(out.toByteArray()).build();
            //return Response.ok("NOT IMPLEMENTED").status(Status.NOT_IMPLEMENTED).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok(WRITING_ERROR_MSG).status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

}
