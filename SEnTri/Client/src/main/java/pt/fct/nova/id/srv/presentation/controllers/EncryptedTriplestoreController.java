package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.core.Response;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.query.SortCondition;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ResultSetStream;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingComparator;
import pt.fct.nova.id.srv.application.clients.EncryptedTriplestoreClient;
import pt.fct.nova.id.srv.application.clients.HTTPResponse;
import pt.fct.nova.id.srv.application.clients.ProxyClient;
import pt.fct.nova.id.srv.application.clients.VaultClient;
import pt.fct.nova.id.srv.application.protocols.Protocol1;
import pt.fct.nova.id.srv.application.protocols.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.query.jobs.SerializableSortCondition;
import pt.fct.nova.id.srv.application.query.plans.DefaultQueryExecutionPlan;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

import static jakarta.ws.rs.core.Response.Status.OK;

public class EncryptedTriplestoreController {

    public Response getEmptySPARQLQueryResult(DefaultQueryExecutionPlan plan, Map<Var, Var> obfuscationMap) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            List<Var> vars = new LinkedList<>();
            for (Var var : plan.getVars())
                vars.add(obfuscationMap.get(var));
            List<Binding> emptyRes = new ArrayList<>(0);
            ResultSetFormatter.outputAsJSON(out, ResultSetStream.create(plan.getVars(), emptyRes.iterator()));
            return Response.ok(out.toByteArray()).build();
        }
    }

    public Collection<Binding> orderResultsIfNeeded(boolean isOrdered, boolean isDistinct, List<SerializableSortCondition> serializableSortConditions, Collection<Binding> bindings) {
        if (isOrdered) {
            List<SortCondition> sortConditions = new LinkedList<>();
            for (SerializableSortCondition condition : serializableSortConditions) {
                sortConditions.add(new SortCondition(condition.getVar(), condition.getDir()));
            }
            if (isDistinct) {
                Collection<Binding> res = new TreeSet<>(new BindingComparator(sortConditions));
                res.addAll(bindings);
                return res;
            } else
                return bindings.stream().sorted(new BindingComparator(sortConditions))
                        .collect(Collectors.toList());
        }
        return bindings;
    }

    public HTTPResponse fetchAndUpdateKeywords(HttpClient httpClient, String triplestoreID, Protocol1 protocol, Set<Map.Entry<String, String>> keywordTrapdoors, String accessToken) throws InvalidNodeException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, IOException, InvalidKeyException {
        List<String> trapdoors = new ArrayList<>(keywordTrapdoors.size());
        List<String> keywords = new ArrayList<>(keywordTrapdoors.size());
        int i = 0;
        for (Map.Entry<String, String> entry : keywordTrapdoors) {
            trapdoors.add(entry.getKey());
            keywords.add(i, entry.getValue());
            i++;
        }

        HTTPResponse response = searchEncryptedTriplestoreContents(httpClient, triplestoreID, trapdoors, accessToken);
        if (response.getStatus() != OK)
            return response;

        List<String> keywordsTotals = ParsingUtils.parseListOfStrings(response.getBody());
        protocol.updateKeywords(protocol.generateKeywordIVMap(keywords, keywordsTotals));
        return null;
    }

    public HTTPResponse query(CloseableHttpClient httpClient, SecretKey secretKey, DefaultQueryExecutionPlan plan, String accessToken) throws IOException {
        try (CloseableHttpResponse response = ProxyClient.query(httpClient, secretKey, plan, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    public HTTPResponse prepareSearch(CloseableHttpClient httpClient, String triplestoreID, List<String> trapdoors, String accessToken) throws IOException {
        try (CloseableHttpResponse response = EncryptedTriplestoreClient.prepareSearch(httpClient, triplestoreID, trapdoors, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    public HTTPResponse searchEncryptedTriplestoreContents(HttpClient httpClient, String triplestoreID, List<String> trapdoors, String accessToken) throws IOException {
        try (CloseableHttpResponse response = EncryptedTriplestoreClient.search(httpClient, triplestoreID, trapdoors, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    public HTTPResponse getProtocolSecrets(CloseableHttpClient httpClient, String triplestoreID, String accessToken) throws IOException {
        try (CloseableHttpResponse response = VaultClient.getProtocolSecrets(httpClient, triplestoreID, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    public HTTPResponse uploadEncryptedTriplestoreContents(CloseableHttpClient httpClient, String triplestoreID, Map<String, String> encryptedT, String accessToken) throws IOException {
        try (CloseableHttpResponse response = EncryptedTriplestoreClient.upload(httpClient, triplestoreID, encryptedT, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    public HTTPResponse saveProtocolSecrets(CloseableHttpClient httpClient, String triplestoreID, Map<String, String> secrets, String accessToken) throws IOException {
        try (CloseableHttpResponse response = VaultClient.saveProtocolSecrets(httpClient, triplestoreID, secrets, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    public HTTPResponse deleteProtocolSecrets(CloseableHttpClient httpClient, String triplestoreID, String accessToken) throws IOException {
        try (CloseableHttpResponse response = VaultClient.deleteProtocolSecrets(httpClient, triplestoreID, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    public HTTPResponse deleteEncryptedTriplestoreContents(CloseableHttpClient httpClient, String triplestoreID, String accessToken) throws IOException {
        try (CloseableHttpResponse response = EncryptedTriplestoreClient.deleteAll(httpClient, triplestoreID, accessToken)) {
            return new HTTPResponse(response);
        }
    }
}
