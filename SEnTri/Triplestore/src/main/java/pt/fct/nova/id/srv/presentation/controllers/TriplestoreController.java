package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.jena.atlas.lib.NotImplemented;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;

import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.sparql.graph.GraphFactory;
import pt.fct.nova.id.srv.application.clients.HTTPClient;
import pt.fct.nova.id.srv.application.clients.HTTPUtils;
import pt.fct.nova.id.srv.application.clients.IAMClient;
import pt.fct.nova.id.srv.application.query.execution.DefaultSPARQLExecution;
import pt.fct.nova.id.srv.application.query.execution.DefaultSPARQLWorker;
import pt.fct.nova.id.srv.application.query.execution.SPARQLExecution;
import pt.fct.nova.id.srv.application.query.plans.QueryExecutionPlan;
import pt.fct.nova.id.srv.application.storage.StorageEngine;
import pt.fct.nova.id.srv.application.storage.redis.RedisDefaultStorageEngine;
import pt.fct.nova.id.srv.presentation.api.TriplestoreAPI;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.List;
import java.util.Set;

import static jakarta.ws.rs.core.Response.Status.*;
import static org.apache.jena.ontology.OntModelSpec.OWL_DL_MEM_TRANS_INF;
import static pt.fct.nova.id.srv.application.clients.HTTPUtils.extractAccessToken;

@Path("")
public class TriplestoreController implements TriplestoreAPI {
    public static final String NO_ACCESS_TOKEN = "Malformed request: bearer token required.";
    public static final String INTERNAL_ERROR = "Internal error.";
    public static final String SUCCESSFUL_UPLOAD = "Successful upload.";
    public static final String SUCCESSFUL_DELETION = "Successful deletion.";
    public static final String NOT_IMPLEMENTED_ERROR = "Operation not yet supported.";
    private static final StorageEngine storageEngine = new RedisDefaultStorageEngine();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder();


    @Override
    public Response upload(String triplestoreID, byte[] triplesData, boolean isSchema, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.hasWriteAccess(httpClient, triplestoreID, accessToken)) {
            if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                return HTTPUtils.buildResponse(response);
            try (ByteArrayInputStream bis = new ByteArrayInputStream(triplesData);
                 ObjectInputStream ois = new ObjectInputStream(bis)){
                Set<Triple> triples = (Set<Triple>) ois.readObject();
                if (isSchema)
                    storageEngine.saveSchema(triplestoreID, triples);
                else {
                    OntModel base = ModelFactory.createOntologyModel(OWL_DL_MEM_TRANS_INF);
                    Graph schema = GraphFactory.createDefaultGraph();
                    Graph data = GraphFactory.createDefaultGraph();
                    storageEngine.findSchema(triplestoreID).forEach(schema::add);
                    triples.forEach(data::add);
                    base.add(ModelFactory.createModelForGraph(data));
                    InfModel infModel = ModelFactory.createInfModel(ReasonerRegistry.getOWLReasoner().bindSchema(schema), base);
                    for (Statement statement : infModel.getDeductionsModel().listStatements().toSet()) {
                        System.out.println(statement);
                        triples.add(statement.asTriple());
                    }
                    storageEngine.save(triplestoreID, triples);
                }
                return Response.ok(SUCCESSFUL_UPLOAD).build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok(INTERNAL_ERROR).status(Status.INTERNAL_SERVER_ERROR).build();
        }

    }

    public Response answerSPARQLQuery(String triplestoreID, byte[] queryExecutionPlan, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.hasReadAccess(httpClient, triplestoreID, accessToken)) {
            if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
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
            return Response.ok(INTERNAL_ERROR).status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response delete(String triplestoreID, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();
        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.hasOwnerAccess(httpClient, triplestoreID, accessToken)) {
            if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                return HTTPUtils.buildResponse(response);
            storageEngine.delete(triplestoreID);
            return Response.ok(SUCCESSFUL_DELETION).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Status.INTERNAL_SERVER_ERROR).build();
        }

    }

    @Override
    public Response delete(String triplestoreID, byte[] triples, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.hasWriteAccess(httpClient, triplestoreID, accessToken)) {
            if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                return HTTPUtils.buildResponse(response);
            try (ByteArrayInputStream bis = new ByteArrayInputStream(triples);
                 ObjectInputStream ois = new ObjectInputStream(bis)) {
                storageEngine.delete(triplestoreID, (Set<Triple>) ois.readObject());
                return Response.ok(SUCCESSFUL_DELETION).build();

            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok(INTERNAL_ERROR).status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
