package pt.fct.nova.id.srv.presentation.controllers;


import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.jena.atlas.lib.NotImplemented;
import pt.fct.nova.id.srv.application.clients.HTTPClient;
import pt.fct.nova.id.srv.application.clients.HTTPUtils;
import pt.fct.nova.id.srv.application.clients.IAMClient;
import pt.fct.nova.id.srv.application.crypto.SymmetricEncryptionUtils;
import pt.fct.nova.id.srv.application.query.execution.DefaultSPARQLExecutionV1;
import pt.fct.nova.id.srv.application.query.execution.SPARQLExecutionV1;
import pt.fct.nova.id.srv.application.query.execution.SecureSPARQLWorkerV1;
import pt.fct.nova.id.srv.application.query.plans.QueryExecutionPlan;
import pt.fct.nova.id.srv.application.storage.redis.ProxyStorage;
import pt.fct.nova.id.srv.application.storage.redis.ProxyStorageV1;
import pt.fct.nova.id.srv.presentation.apis.QueriesAPI;
import pt.fct.nova.id.srv.presentation.dtos.SecureSPARQLQueryForm;

import javax.crypto.SecretKey;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.List;

import static pt.fct.nova.id.srv.application.clients.HTTPUtils.extractAccessToken;
import static jakarta.ws.rs.core.Response.Status.*;

@Path("/queries/v1")
public class QueriesV1Controller implements QueriesAPI {
    public static final String NO_ACCESS_TOKEN = "Malformed request: bearer token required.";
    public static final String INTERNAL_ERROR = "Internal error.";
    public static final String NOT_IMPLEMENTED_ERROR = "Operation not yet supported.";

    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder();

    @Override
    public Response answerSPARQLQuery(SecureSPARQLQueryForm form, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.checkIfActive(httpClient, accessToken);
             ByteArrayInputStream plan_is = new ByteArrayInputStream(form.getQueryExecutionPlan());
             ObjectInputStream plan_ois = new ObjectInputStream(plan_is)) {

            if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                return HTTPUtils.buildResponse(response);

            QueryExecutionPlan executionPlan = (QueryExecutionPlan) plan_ois.readObject();
            SecretKey secretKey = SymmetricEncryptionUtils.parseKey(form.getKey());

            SPARQLExecutionV1 execution = new DefaultSPARQLExecutionV1(executionPlan);
            SecureSPARQLWorkerV1 worker = new SecureSPARQLWorkerV1(secretKey);
            execution.exec(worker);
            ProxyStorage.delete(worker.getAllSearchIDs());
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                 ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                oos.writeObject(execution.getResults());
                return Response.ok(base64Encoder.encodeToString(bos.toByteArray())).build();
            }
        } catch (NotImplemented e) {
            return Response.ok(NOT_IMPLEMENTED_ERROR).status(NOT_IMPLEMENTED).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response prepareSearch(byte[] encryptedNodes, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();
        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.checkIfActive(httpClient, accessToken);
             ByteArrayInputStream is = new ByteArrayInputStream(encryptedNodes);
             ObjectInputStream ois = new ObjectInputStream(is)) {
            if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                return HTTPUtils.buildResponse(response);
            return Response.ok(base64Encoder.encodeToString(ProxyStorageV1.save((List<byte[]>) ois.readObject()))).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
