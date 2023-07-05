package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.query.SortCondition;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ResultSetStream;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingComparator;
import org.json.JSONObject;
import pt.fct.nova.id.srv.application.clients.*;
import pt.fct.nova.id.srv.application.query.QueryUtils;
import pt.fct.nova.id.srv.application.query.execution.SPARQLResult;
import pt.fct.nova.id.srv.application.query.jobs.SerializableBinding;
import pt.fct.nova.id.srv.application.query.jobs.SerializableSortCondition;
import pt.fct.nova.id.srv.application.query.plans.DefaultQueryExecutionPlan;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static jakarta.ws.rs.core.Response.Status.OK;
import static pt.fct.nova.id.srv.presentation.controllers.TriplestoreController.*;
import static pt.fct.nova.id.srv.presentation.controllers.TriplestoreController.deleteAccessToken;

public class EncryptedTriplestoreController {

    public static final String SECRETS_KEY = System.getenv("SECRETS_PROTOCOL_KEY");
    public static final String SECRETS_IV = System.getenv("SECRETS_PROTOCOL_IV");
    public static final String SECRETS_SCHEMA_KEYWORD = System.getenv("SECRETS_PROTOCOL_SCHEMA_KEYWORD");
    public static final String SUCCESSFUL_CREATION = "Successful creation.";
    public static final String EMPTY_UPLOAD = "No content to upload.";
    public static final String BAD_NODE = "Data must only contain concrete nodes: IRI, Blank, Literal.";
    public static final String NO_UPDATES = "No content to update.";
    public static final int MIN_TRAPDOORS = Integer.parseInt(System.getenv("MINIMUM_TRAPDOORS_PER_SEARCH"));
    public static final int MAX_TRAPDOORS = Integer.parseInt(System.getenv("MAXIMUM_TRAPDOORS_PER_SEARCH"));
    public static final int BATCH_SIZE = Integer.parseInt(System.getenv("BATCH_SIZE"));

    public static HTTPResponse deleteEncryptedTriplestore(CloseableHttpClient httpClient, Cookie cookie, String protocolVersion, String triplestoreID, String issuer) throws IOException {
        HTTPResponse response = createAccessToken(httpClient, cookie, issuer, triplestoreID);
        if (response.getStatus() != OK)
            return response;
        String accessToken = response.getBody();

        response = acquireTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
        if (response.getStatus() != OK) {
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return response;
        }

        response = deleteAllContents(httpClient, protocolVersion, triplestoreID, accessToken);
        if (response.getStatus() != OK) {
            releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
        }
        response = deleteProtocolSecrets(httpClient, triplestoreID, accessToken);
        if (response.getStatus() != OK) {
            releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
        }
        response = deleteTriplestoreAccessPolicy(httpClient, cookie, triplestoreID, accessToken);
        if (response.getStatus() != OK) {
            releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return response;
        }
        return response;
    }

    public Collection<Binding> orderResults(boolean isDistinct, List<SerializableSortCondition> serializableSortConditions,
                                            Map<Var, Var> obfuscationMap, Collection<Binding> bindings) {
        List<SortCondition> sortConditions = new LinkedList<>();
        for (SerializableSortCondition condition : serializableSortConditions)
            sortConditions.add(new SortCondition(obfuscationMap.get(condition.getVar()), condition.getDir()));

        if (isDistinct) {
            Collection<Binding> res = new TreeSet<>(new BindingComparator(sortConditions));
            res.addAll(bindings);
            return res;
        } else
            return bindings.stream().sorted(new BindingComparator(sortConditions)).collect(Collectors.toList());
    }

    public Collection<Binding> sliceResults(Long offset, Long length, Collection<Binding> bindings) {
        if (offset != Query.NOLIMIT && length != Query.NOLIMIT)
            return bindings.stream().skip(offset).limit(length).collect(Collectors.toList());
        else if (offset != Query.NOLIMIT)
            return bindings.stream().skip(offset).collect(Collectors.toList());
        else if (length != Query.NOLIMIT)
            return bindings.stream().limit(length).collect(Collectors.toList());
        else return bindings;
    }

    public static Response generateASKResults(SPARQLResult<byte[]> sparqlResult) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ResultSetFormatter.outputAsJSON(out, !sparqlResult.getBindings().isEmpty());
            return Response.ok(out.toByteArray()).build();
        }
    }

    public Response generateDESCRIBEResults(List<Var> vars, Map<Var, Var> obfuscationMap, SPARQLResult<byte[]> sparqlResult) {
        Map<Var, Integer> frequencies = new HashMap<>();
        for (Var v : vars)
            frequencies.put(v, 0);
        for (SerializableBinding<byte[]> binding : sparqlResult.getBindings()) {
            for (Iterator<Var> it = binding.vars(); it.hasNext(); ) {
                Var v = it.next();
                frequencies.put(v, frequencies.get(v) + 1);
            }
        }
        JSONObject responseBody = new JSONObject();
        for (Var v : vars)
            responseBody = responseBody.put(obfuscationMap.get(v).getVarName(), frequencies.get(v));
        return Response.ok(responseBody.toString()).build();
    }

    public Response generateCONSTRUCTResults(List<Triple> constructTemplate, Collection<Binding> bindings) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Graph g = QueryUtils.generateGraphFromBindings(constructTemplate, bindings);
            RDFWriter.create(g).lang(Lang.JSONLD).output(out);
            return Response.ok(out.toByteArray()).build();
        }
    }

    public Response generateSELECTResults(List<Var> vars, Collection<Binding> bindings) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ResultSetFormatter.outputAsJSON(out, ResultSetStream.create(vars, bindings.iterator()));
            return Response.ok(out.toByteArray()).build();
        }
    }

    public Response updateTriplestore(CloseableHttpClient httpClient, Cookie cookie, String protocolVersion, String triplestoreID,
                                      Set<String> deletions, Set<String> uploads, String accessToken) throws IOException {
        HTTPResponse response = execTriplestoreUpdate(httpClient, protocolVersion, triplestoreID, deletions, uploads, accessToken);
        if (response.getStatus() != OK) {
            releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return response.build();
        }
        releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
        deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
        return Response.ok(SUCCESSFUL_UPDATE).build();
    }

    public HTTPResponse query(HttpClient httpClient, String protocolVersion, byte[] keyBytes, DefaultQueryExecutionPlan plan, String accessToken) throws IOException {
        try (CloseableHttpResponse response = ProxyClient.query(httpClient, protocolVersion, keyBytes, plan, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    public HTTPResponse searchEncryptedTriplestoreContents(HttpClient httpClient, String protocolVersion, String triplestoreID, List<String> trapdoors, String accessToken) throws IOException {
        try (CloseableHttpResponse response = EncryptedTriplestoreClient.search(httpClient, protocolVersion, triplestoreID, trapdoors, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    public HTTPResponse getProtocolSecrets(HttpClient httpClient, String triplestoreID, String accessToken) throws IOException {
        try (CloseableHttpResponse response = VaultClient.getProtocolSecrets(httpClient, triplestoreID, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    public HTTPResponse upload(CloseableHttpClient httpClient, String protocolVersion, String triplestoreID, Map<String, String> encryptedNodes, String accessToken) throws IOException {
        try (CloseableHttpResponse response = EncryptedTriplestoreClient.upload(httpClient, protocolVersion, triplestoreID, encryptedNodes, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    public HTTPResponse saveProtocolSecrets(CloseableHttpClient httpClient, String triplestoreID, Map<String, String> secrets, String accessToken) throws IOException {
        try (CloseableHttpResponse response = VaultClient.saveProtocolSecrets(httpClient, triplestoreID, secrets, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    public static HTTPResponse deleteProtocolSecrets(CloseableHttpClient httpClient, String triplestoreID, String accessToken) throws IOException {
        try (CloseableHttpResponse response = VaultClient.deleteProtocolSecrets(httpClient, triplestoreID, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    public static HTTPResponse deleteAllContents(CloseableHttpClient httpClient, String protocolVersion, String triplestoreID, String accessToken) throws IOException {
        try (CloseableHttpResponse response = EncryptedTriplestoreClient.deleteAll(httpClient, protocolVersion, triplestoreID, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    public static HTTPResponse deleteSomeContents(CloseableHttpClient httpClient, String protocolVersion, String triplestoreID, Set<String> trapdoors, String accessToken) throws IOException {
        try (CloseableHttpResponse response = EncryptedTriplestoreClient.deleteSome(httpClient, protocolVersion, triplestoreID, trapdoors, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    public static HTTPResponse execTriplestoreUpdate(CloseableHttpClient httpClient, String protocolVersion, String triplestoreID,
                                                     Set<String> deletions, Set<String> uploads, String accessToken) throws IOException {
        try (CloseableHttpResponse response = EncryptedTriplestoreClient.update(httpClient, protocolVersion, triplestoreID,
                deletions, uploads, accessToken)) {
            return new HTTPResponse(response);
        }
    }

}
