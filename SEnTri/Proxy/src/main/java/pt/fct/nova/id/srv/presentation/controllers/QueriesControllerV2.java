package pt.fct.nova.id.srv.presentation.controllers;


import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.jena.atlas.lib.NotImplemented;
import pt.fct.nova.id.srv.application.clients.HTTPClient;
import pt.fct.nova.id.srv.application.clients.HTTPUtils;
import pt.fct.nova.id.srv.application.clients.IAMClient;
import pt.fct.nova.id.srv.application.crypto.dgk.DGKEqKey;
import pt.fct.nova.id.srv.application.query.execution.*;
import pt.fct.nova.id.srv.application.query.plans.QueryExecutionPlan;
import pt.fct.nova.id.srv.application.storage.redis.ProxyStorage;
import pt.fct.nova.id.srv.application.storage.redis.ProxyStorageV2;
import pt.fct.nova.id.srv.presentation.apis.QueriesAPI;
import pt.fct.nova.id.srv.presentation.dtos.SecureSPARQLQueryForm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.List;

import static jakarta.ws.rs.core.Response.Status.*;
import static pt.fct.nova.id.srv.application.clients.HTTPUtils.extractAccessToken;
import static pt.fct.nova.id.srv.presentation.controllers.QueriesControllerV1.*;

@Path("/queries/v2")
public class QueriesControllerV2 implements QueriesAPI {
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder();

    @Override
    public Response answerSPARQLQuery(SecureSPARQLQueryForm form, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.checkIfActive(httpClient, accessToken);
             ByteArrayInputStream plan_is = new ByteArrayInputStream(form.getQueryExecutionPlan());
             ObjectInputStream plan_ois = new ObjectInputStream(plan_is);
             ByteArrayInputStream eqKey_is = new ByteArrayInputStream(form.getKey());
             ObjectInputStream eqKey_ois = new ObjectInputStream(eqKey_is)) {

            if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                return HTTPUtils.buildResponse(response);

            QueryExecutionPlan executionPlan = (QueryExecutionPlan) plan_ois.readObject();
            DGKEqKey eqKey = (DGKEqKey) eqKey_ois.readObject();

            SPARQLExecutionV2 execution = new DefaultSPARQLExecutionV2(executionPlan);
            SecureSPARQLWorkerV2 worker = new SecureSPARQLWorkerV2(eqKey);
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
            return Response.ok(base64Encoder.encodeToString(ProxyStorageV2.save((List<byte[]>) ois.readObject()))).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
