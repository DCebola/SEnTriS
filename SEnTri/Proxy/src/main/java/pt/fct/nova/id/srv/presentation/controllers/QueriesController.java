package pt.fct.nova.id.srv.presentation.controllers;


import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ResultSetStream;
import org.apache.jena.sparql.engine.binding.Binding;
import pt.fct.nova.id.srv.application.query.execution.DefaultSPARQLExecution;
import pt.fct.nova.id.srv.application.query.execution.SPARQLExecution;
import pt.fct.nova.id.srv.application.query.execution.SecureSPARQLWorker;
import pt.fct.nova.id.srv.application.query.plans.QueryExecutionPlan;
import pt.fct.nova.id.srv.presentation.api.ProxyAPI;
import pt.fct.nova.id.srv.presentation.api.dtos.SecureSPARQLQueryForm;

import javax.crypto.SecretKey;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.util.Iterator;
import java.util.List;

import static jakarta.ws.rs.core.Response.Status.*;

@Path("/queries")
public class QueriesController implements ProxyAPI {
    public static final String INTERNAL_ERROR = "Internal error.";
    public static final String NOT_IMPLEMENTED_ERROR = "Operation not yet supported.";

    @Override
    public Response answerSPARQLQuery(SecureSPARQLQueryForm form) {

        try (ByteArrayInputStream plan_is = new ByteArrayInputStream(form.getQueryExecutionPlan());
             ObjectInputStream plan_ois = new ObjectInputStream(plan_is);
             ByteArrayInputStream key_is = new ByteArrayInputStream(form.getKey());
             ObjectInputStream key_ois = new ObjectInputStream(key_is)) {

            QueryExecutionPlan executionPlan = (QueryExecutionPlan) plan_ois.readObject();
            SecretKey secretKey = (SecretKey) key_ois.readObject();

            SPARQLExecution execution = new DefaultSPARQLExecution(executionPlan);
            execution.exec(new SecureSPARQLWorker(secretKey));
            return Response.ok(execution.getResults()).build();
        } catch (NotImplemented e) {
            return Response.ok(NOT_IMPLEMENTED_ERROR).status(NOT_IMPLEMENTED).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response saveBinding(List<String> encryptedNodes) {
        return null;
    }
}
