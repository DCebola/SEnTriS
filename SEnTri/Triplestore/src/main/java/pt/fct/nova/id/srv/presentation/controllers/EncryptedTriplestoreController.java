package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import pt.fct.nova.id.srv.application.query.execution.SimpleSPARQLExecution;
import pt.fct.nova.id.srv.application.query.plans.QueryExecutionPlan;
import pt.fct.nova.id.srv.application.storage.EncryptedStorageEngine;
import pt.fct.nova.id.srv.application.storage.exceptions.StoreAlreadyExistsException;
import pt.fct.nova.id.srv.application.storage.exceptions.StoreNotFoundException;
import pt.fct.nova.id.srv.application.storage.redis.EncryptedRStorageEngine;
import pt.fct.nova.id.srv.application.triplestores.EncryptedTriplestore;
import pt.fct.nova.id.srv.application.triplestores.EncryptedTriplestoreImpl;
import pt.fct.nova.id.srv.presentation.api.EncryptedTriplestoreAPI;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@Path("secure-triplestore")
public class EncryptedTriplestoreController implements EncryptedTriplestoreAPI {
    private static final String SAVE_ERROR_MSG = "Error saving encrypted contents.";
    private static final String SUCCESS_UPLOAD = "Successful upload.";
    private static final String SEARCH_ERROR_MSG = "Error while executing search.";
    private static final String STORE_ALREADY_EXISTS = "Store %s already exists.";
    private static final String STORE_NOT_FOUND = "Store %s not found.";
    private static final String SUCCESS_DELETE = "Store %s deleted.";

    EncryptedStorageEngine storageEngine = new EncryptedRStorageEngine();
    EncryptedTriplestore encryptedTriplestore = new EncryptedTriplestoreImpl(storageEngine);

    @Override
    public Response create(String storeID, Map<String, String> encryptedNodes) {
        try {
            encryptedTriplestore.createDataset(storeID, encryptedNodes);
            return Response.ok(SUCCESS_UPLOAD).build();
        } catch (StoreAlreadyExistsException e) {
            return Response.ok(String.format(STORE_ALREADY_EXISTS, storeID)).status(Response.Status.BAD_REQUEST).build();
        } catch (Exception e) {
            return Response.ok(SAVE_ERROR_MSG).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response upload(String storeID, Map<String, String> encryptedNodes) {
        try {
            encryptedTriplestore.uploadData(storeID, encryptedNodes);
            return Response.ok(SUCCESS_UPLOAD).build();
        } catch (StoreNotFoundException e) {
            return Response.ok(String.format(STORE_NOT_FOUND, storeID)).status(Response.Status.NOT_FOUND).build();
        } catch (Exception e) {
            return Response.ok(SAVE_ERROR_MSG).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response search(String storeID, List<String> trapdoors) {
        try {
            return Response.ok(storageEngine.search(storeID, trapdoors)).build();
        } catch (StoreNotFoundException e) {
            return Response.ok(String.format(STORE_NOT_FOUND, storeID)).status(Response.Status.NOT_FOUND).build();
        } catch (Exception e) {
            return Response.ok(SEARCH_ERROR_MSG).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response delete(String storeID) {
        try {
            encryptedTriplestore.delete(storeID);
            return Response.ok(String.format(SUCCESS_DELETE, storeID)).build();
        } catch (StoreNotFoundException e) {
            return Response.ok(String.format(STORE_NOT_FOUND, storeID)).status(Response.Status.NOT_FOUND).build();
        }
    }
}
