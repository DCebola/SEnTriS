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

import javax.crypto.SecretKey;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

import static jakarta.ws.rs.core.Response.Status.OK;
import static pt.fct.nova.id.srv.presentation.controllers.TriplestoreController.*;
import static pt.fct.nova.id.srv.presentation.controllers.TriplestoreController.deleteAccessToken;

public class EncryptedTriplestoreController {
    public static HTTPResponse deleteEncryptedTriplestore(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID, String issuer) throws IOException {
        HTTPResponse response = createAccessToken(httpClient, cookie, issuer, triplestoreID);
        if (response.getStatus() != OK)
            return response;
        String accessToken = response.getBody();

        response = acquireTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
        if (response.getStatus() != OK) {
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return response;
        }

        response = deleteAllContents(httpClient, triplestoreID, accessToken);
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

    public Response getEmptySPARQLQueryResult(DefaultQueryExecutionPlan plan, Map<Var, Var> obfuscationMap) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            List<Var> vars = new LinkedList<>();
            for (Var var : plan.getVars())
                vars.add(obfuscationMap.get(var));
            ResultSetFormatter.outputAsJSON(out, ResultSetStream.create(vars, Collections.emptyIterator()));
            return Response.ok(out.toByteArray()).build();
        }
    }

    public List<Integer> generateRandomPermutation(int total) {
        List<Integer> idxs = new ArrayList<>(total);
        for (int i = 0; i < total; i++) idxs.add(i);
        Collections.shuffle(idxs);
        return idxs;
    }

    public Collection<Binding> orderResults(boolean isDistinct, List<SerializableSortCondition> serializableSortConditions, Map<Var, Var> obfuscationMap, Collection<Binding> bindings) {
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

    public Response generateDESCRIBEResults(List<Var> vars, Map<Var, Var> obfuscationMap, SPARQLResult sparqlResult) {
        Map<Var, Integer> frequencies = new HashMap<>();
        for (Var v : vars)
            frequencies.put(v, 0);
        for (SerializableBinding binding : sparqlResult.getBindings()) {
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

    public Response updateTriplestore(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID, Map<String, String> uploads, Set<String> deletions,
                                      Map<String, String> swaps, String accessToken) throws IOException {
        System.out.println("UPLOADS: " + uploads.size());
        System.out.println("DELETIONS: " + deletions.size());
        System.out.println("SWAPS: " + swaps.size());
        HTTPResponse response;
        if (!deletions.isEmpty()){
            response = deleteSomeContents(httpClient, triplestoreID, deletions, accessToken);
            if(response.getStatus() != OK) {
                releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }
        }
        if (!swaps.isEmpty()){
            response = swapSomeContents(httpClient, triplestoreID, swaps, accessToken);
            if(response.getStatus() != OK) {
                releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }
        }
        if (!uploads.isEmpty()) {
            response = upload(httpClient, triplestoreID, uploads, accessToken);
            if(response.getStatus() != OK) {
                releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }
        }
        releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
        deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
        return Response.ok(SUCCESSFUL_UPDATE).build();
    }

    private HTTPResponse swapSomeContents(CloseableHttpClient httpClient, String triplestoreID, Map<String, String> swaps, String accessToken) throws IOException {
        try (CloseableHttpResponse response = EncryptedTriplestoreClient.swap(httpClient, triplestoreID, swaps, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    public HTTPResponse query(HttpClient httpClient, SecretKey secretKey, DefaultQueryExecutionPlan plan, String accessToken) throws IOException {
        try (CloseableHttpResponse response = ProxyClient.query(httpClient, secretKey, plan, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    public HTTPResponse prepareSearch(CloseableHttpClient httpClient, String triplestoreID, List<String> trapdoors, String accessToken) throws IOException, URISyntaxException {
        try (CloseableHttpResponse response = EncryptedTriplestoreClient.prepareSearch(httpClient, triplestoreID, trapdoors, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    public HTTPResponse searchEncryptedTriplestoreContents(HttpClient httpClient, String triplestoreID, List<String> trapdoors, String accessToken) throws IOException {
        try (CloseableHttpResponse response = EncryptedTriplestoreClient.search(httpClient, triplestoreID, trapdoors, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    public HTTPResponse getProtocolSecrets(HttpClient httpClient, String triplestoreID, String accessToken) throws IOException {
        try (CloseableHttpResponse response = VaultClient.getProtocolSecrets(httpClient, triplestoreID, accessToken)) {
            return new HTTPResponse(response);
        }
    }


    public HTTPResponse upload(CloseableHttpClient httpClient, String triplestoreID, Map<String, String> encryptedNodes, String accessToken) throws IOException {
        try (CloseableHttpResponse response = EncryptedTriplestoreClient.upload(httpClient, triplestoreID, encryptedNodes, accessToken)) {
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

    public static HTTPResponse deleteAllContents(CloseableHttpClient httpClient, String triplestoreID, String accessToken) throws IOException {
        try (CloseableHttpResponse response = EncryptedTriplestoreClient.deleteAll(httpClient, triplestoreID, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    public static HTTPResponse deleteSomeContents(CloseableHttpClient httpClient, String triplestoreID, Set<String> trapdoors, String accessToken) throws IOException {
        try (CloseableHttpResponse response = EncryptedTriplestoreClient.deleteSome(httpClient, triplestoreID, trapdoors, accessToken)) {
            return new HTTPResponse(response);
        }
    }
}
