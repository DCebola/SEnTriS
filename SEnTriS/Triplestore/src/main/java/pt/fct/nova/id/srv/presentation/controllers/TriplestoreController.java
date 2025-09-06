package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.graph.Triple;
import pt.fct.nova.id.srv.application.clients.HTTPClient;
import pt.fct.nova.id.srv.application.clients.HTTPUtils;
import pt.fct.nova.id.srv.application.clients.IAMClient;
import pt.fct.nova.id.srv.application.query.execution.DefaultSPARQLExecution;
import pt.fct.nova.id.srv.application.query.execution.DefaultSPARQLWorker;
import pt.fct.nova.id.srv.application.query.execution.SPARQLExecution;
import pt.fct.nova.id.srv.application.query.plans.QueryExecutionPlan;
import pt.fct.nova.id.srv.application.storage.StorageEngine;
import pt.fct.nova.id.srv.application.storage.redis.TriplestoreStorageEngine;
import pt.fct.nova.id.srv.presentation.apis.TriplestoreAPI;

import java.io.*;
import java.util.*;

import static jakarta.ws.rs.core.Response.Status.*;
import static pt.fct.nova.id.srv.application.clients.HTTPUtils.extractAccessToken;

@Path("")
public class TriplestoreController implements TriplestoreAPI {
    public static final String SUCCESSFUL_UPLOAD = "Successful upload.";
    public static final String SUCCESSFUL_DELETION = "Successful deletion.";
    public static final String NOT_IMPLEMENTED_ERROR = "Operation not yet supported.";
    private static final StorageEngine storageEngine = new TriplestoreStorageEngine();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder();


    @Override
    public Response upload(String triplestoreID, byte[] triplesData, boolean schema, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.status(UNAUTHORIZED).build();

        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.hasWriteAccess(httpClient, triplestoreID, accessToken)) {
            if (response.getCode() != OK.getStatusCode())
                return HTTPUtils.buildResponse(response);
            try (ByteArrayInputStream bis = new ByteArrayInputStream(triplesData);
                 ObjectInputStream ois = new ObjectInputStream(bis)) {
                Set<Triple> triples = (Set<Triple>) ois.readObject();
                System.out.println(schema + " | " + triples.size());
                if (schema) {
                    storageEngine.delete(triplestoreID, true);
                    storageEngine.saveSchema(triplestoreID, triples);
                } else
                    storageEngine.save(triplestoreID, triples);
                return Response.ok(SUCCESSFUL_UPLOAD).build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }

    }

    @Override
    public Response fetchInfo(String triplestoreID, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.status(UNAUTHORIZED).build();
        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.hasReadAccess(httpClient, triplestoreID, accessToken)) {
            if (response.getCode() != OK.getStatusCode())
                return HTTPUtils.buildResponse(response);
            return Response.ok(storageEngine.memoryUsage(triplestoreID)).build();
        } catch (NotImplemented e) {
            return Response.ok(NOT_IMPLEMENTED_ERROR).status(NOT_IMPLEMENTED).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response fetchSchema(String triplestoreID, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.status(UNAUTHORIZED).build();
        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.hasReadAccess(httpClient, triplestoreID, accessToken)) {
            if (response.getCode() != OK.getStatusCode())
                return HTTPUtils.buildResponse(response);
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                oos.writeObject(storageEngine.findSchema(triplestoreID));
                return Response.ok(base64Encoder.encodeToString(bos.toByteArray())).build();
            }
        } catch (NotImplemented e) {
            return Response.ok(NOT_IMPLEMENTED_ERROR).status(NOT_IMPLEMENTED).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }
    }

    public Response answerSPARQLQuery(String triplestoreID, byte[] queryExecutionPlan, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.status(UNAUTHORIZED).build();

        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.hasReadAccess(httpClient, triplestoreID, accessToken)) {
            if (response.getCode() != OK.getStatusCode())
                return HTTPUtils.buildResponse(response);

            try (ByteArrayInputStream bis = new ByteArrayInputStream(queryExecutionPlan);
                 ObjectInputStream ois = new ObjectInputStream(bis);
                 ByteArrayOutputStream bos = new ByteArrayOutputStream();
                 ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                QueryExecutionPlan executionPlan = (QueryExecutionPlan) ois.readObject();
                SPARQLExecution execution = new DefaultSPARQLExecution(executionPlan);
                execution.exec(new DefaultSPARQLWorker(triplestoreID, storageEngine));
                oos.writeObject(execution.getResults());
                return Response.ok(base64Encoder.encodeToString(bos.toByteArray())).build();
            }
        } catch (NotImplemented e) {
            return Response.ok(NOT_IMPLEMENTED_ERROR).status(NOT_IMPLEMENTED).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response delete(String triplestoreID, boolean isSchema, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.status(UNAUTHORIZED).build();
        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.hasOwnerAccess(httpClient, triplestoreID, accessToken)) {
            if (response.getCode() != OK.getStatusCode())
                return HTTPUtils.buildResponse(response);
            storageEngine.delete(triplestoreID, isSchema);
            return Response.ok(SUCCESSFUL_DELETION).build();
        } catch (Exception e) {
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }

    }

    @Override
    public Response delete(String triplestoreID, byte[] triplesData, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.status(UNAUTHORIZED).build();

        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.hasWriteAccess(httpClient, triplestoreID, accessToken)) {
            if (response.getCode() != OK.getStatusCode())
                return HTTPUtils.buildResponse(response);
            try (ByteArrayInputStream bis = new ByteArrayInputStream(triplesData);
                 ObjectInputStream ois = new ObjectInputStream(bis)) {
                storageEngine.delete(triplestoreID, (Set<Triple>) ois.readObject());
                return Response.ok(SUCCESSFUL_DELETION).build();

            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }
    }
}
