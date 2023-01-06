package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.jena.query.Query;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.query.SortCondition;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ResultSetStream;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingComparator;
import pt.fct.nova.id.srv.application.clients.*;
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

    public static HTTPResponse testValues(CloseableHttpClient httpClient,String searchID, List<String> values,  String accessToken) throws IOException {
        try(CloseableHttpResponse response = ProxyClient.testValues(httpClient, searchID, values, accessToken)){
            return new HTTPResponse(response);
        }
    }

    public static HTTPResponse deleteSomeContents(CloseableHttpClient httpClient, String triplestoreID, List<String> trapdoors, String accessToken) throws IOException {
        try (CloseableHttpResponse response = EncryptedTriplestoreClient.deleteSome(httpClient, triplestoreID, trapdoors, accessToken)) {
            return new HTTPResponse(response);
        }
    }
}
