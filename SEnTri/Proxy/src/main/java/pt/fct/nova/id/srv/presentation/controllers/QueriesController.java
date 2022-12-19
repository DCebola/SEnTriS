package pt.fct.nova.id.srv.presentation.controllers;


import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.jena.atlas.lib.NotImplemented;
import pt.fct.nova.id.srv.application.clients.HTTPClient;
import pt.fct.nova.id.srv.application.clients.HTTPUtils;
import pt.fct.nova.id.srv.application.clients.IAMClient;
import pt.fct.nova.id.srv.application.query.execution.DefaultSPARQLExecution;
import pt.fct.nova.id.srv.application.query.execution.SPARQLExecution;
import pt.fct.nova.id.srv.application.query.execution.SecureSPARQLWorker;
import pt.fct.nova.id.srv.application.query.plans.QueryExecutionPlan;
import pt.fct.nova.id.srv.application.storage.redis.ProxyStorage;
import pt.fct.nova.id.srv.presentation.api.QueriesAPI;
import pt.fct.nova.id.srv.presentation.api.dtos.SecureSPARQLQueryForm;

import javax.crypto.SecretKey;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.List;

import static pt.fct.nova.id.srv.application.clients.HTTPUtils.extractAccessToken;
import static jakarta.ws.rs.core.Response.Status.*;

@Path("/queries")
public class QueriesController implements QueriesAPI {
    public static final String NO_ACCESS_TOKEN = "Malformed request: bearer token required.";
    public static final String INTERNAL_ERROR = "Internal error.";
    public static final String NOT_IMPLEMENTED_ERROR = "Operation not yet supported.";

    @Override
    public Response answerSPARQLQuery(SecureSPARQLQueryForm form, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.checkIfActive(httpClient, accessToken);
             ByteArrayInputStream plan_is = new ByteArrayInputStream(form.getQueryExecutionPlan());
             ObjectInputStream plan_ois = new ObjectInputStream(plan_is);
             ByteArrayInputStream key_is = new ByteArrayInputStream(form.getKey());
             ObjectInputStream key_ois = new ObjectInputStream(key_is)) {

            if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                return HTTPUtils.buildResponse(response);

            QueryExecutionPlan executionPlan = (QueryExecutionPlan) plan_ois.readObject();
            SecretKey secretKey = (SecretKey) key_ois.readObject();

            SPARQLExecution execution = new DefaultSPARQLExecution(executionPlan);
            SecureSPARQLWorker worker = new SecureSPARQLWorker(secretKey);
            execution.exec(worker);
            ProxyStorage.delete(worker.getAllSearchIDs());
            return Response.ok(execution.getResults()).build();
        } catch (NotImplemented e) {
            return Response.ok(NOT_IMPLEMENTED_ERROR).status(NOT_IMPLEMENTED).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response saveSearchResults(List<String> encryptedNodes, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();
        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.checkIfActive(httpClient, accessToken)) {
            if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                return HTTPUtils.buildResponse(response);
            return Response.ok(ProxyStorage.save(encryptedNodes)).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
