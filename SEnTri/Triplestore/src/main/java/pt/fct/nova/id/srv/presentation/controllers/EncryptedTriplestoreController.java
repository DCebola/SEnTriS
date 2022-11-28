package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import pt.fct.nova.id.srv.application.query.execution.SecureSPARQLWorker;
import pt.fct.nova.id.srv.application.query.execution.SimpleSPARQLExecution;
import pt.fct.nova.id.srv.application.query.plans.QueryExecutionPlan;
import pt.fct.nova.id.srv.application.storage.EncryptedStorageEngine;
import pt.fct.nova.id.srv.application.storage.redis.EncryptedRStorageEngine;
import pt.fct.nova.id.srv.presentation.api.EncryptedTriplestoreAPI;

import java.io.ByteArrayOutputStream;
import java.util.*;

import static jakarta.ws.rs.core.Response.Status.NOT_IMPLEMENTED;
import static pt.fct.nova.id.srv.presentation.controllers.TriplestoreController.*;


@Path("secure")
public class EncryptedTriplestoreController implements EncryptedTriplestoreAPI {

    private static final String SUCCESS_DELETE_BATCH = "Successful deletion of values from store.";

    EncryptedStorageEngine storageEngine = new EncryptedRStorageEngine();

    @Override
    public Response upload(String storeID, Map<String, String> encryptedNodes) {
        try {
            storageEngine.save(storeID, encryptedNodes);
            return Response.ok(SUCCESSFUL_UPLOAD).build();
        } catch (Exception e) {
            return Response.ok(UPLOAD_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response answerSPARQLQuery(String storeID, QueryExecutionPlan queryExecutionPlan) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ResultSet res = new SimpleSPARQLExecution(queryExecutionPlan).exec(new SecureSPARQLWorker(storeID, storageEngine));
            ResultSetFormatter.outputAsJSON(out, res);
            return Response.ok(out.toByteArray()).build();
        } catch (NotImplemented e) {
            return Response.ok(NOT_IMPLEMENTED).status(NOT_IMPLEMENTED).build();
        } catch (Exception e) {
            return Response.ok(QUERY_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response search(String storeID, List<String> trapdoors) {
        return Response.ok(storageEngine.search(storeID, trapdoors)).build();
    }

    @Override
    public Response delete(String storeID) {
        storageEngine.delete(storeID);
        return Response.ok(SUCCESS_DELETE_BATCH).build();
    }

    @Override
    public Response delete(String storeID, List<String> trapdoors) {
        storageEngine.delete(storeID, trapdoors);
        return Response.ok(SUCCESSFUL_DELETION).build();
    }
}
