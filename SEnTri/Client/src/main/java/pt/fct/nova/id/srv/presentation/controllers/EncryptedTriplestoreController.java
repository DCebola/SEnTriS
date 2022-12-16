package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ResultSetStream;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingComparator;
import pt.fct.nova.id.srv.application.QueryEngine;
import pt.fct.nova.id.srv.application.SPARQLQueryEngine;
import pt.fct.nova.id.srv.application.clients.*;
import pt.fct.nova.id.srv.application.protocols.ProtocolVersion;
import pt.fct.nova.id.srv.application.protocols.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.protocols.Protocol1;
import pt.fct.nova.id.srv.application.query.Utils;
import pt.fct.nova.id.srv.application.query.execution.SPARQLResult;
import pt.fct.nova.id.srv.application.query.jobs.SearchJob;
import pt.fct.nova.id.srv.application.query.jobs.SecureSearchJob;
import pt.fct.nova.id.srv.application.query.plans.DefaultQueryExecutionPlan;
import pt.fct.nova.id.srv.application.query.plans.DefaultSPARQLPlanner;
import pt.fct.nova.id.srv.presentation.api.EncryptedTriplestoreAPI;
import pt.fct.nova.id.srv.presentation.api.dtos.EncryptedCreateForm;
import pt.fct.nova.id.srv.presentation.api.dtos.QueryForm;
import pt.fct.nova.id.srv.presentation.api.dtos.UploadForm;
import pt.fct.nova.id.srv.presentation.exceptions.UnknownRDFLanguageException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

import static jakarta.ws.rs.core.Response.Status.*;
import static pt.fct.nova.id.srv.presentation.controllers.ParsingUtils.*;
import static pt.fct.nova.id.srv.presentation.controllers.TriplestoreController.INVALID_SYNTAX;

@Path("triplestores/encrypted")
public class EncryptedTriplestoreController implements EncryptedTriplestoreAPI {
    public static final String SECRETS_VERSION = System.getenv("SECRETS_PROTOCOL_VERSION");
    public static final String SECRETS_KEY = System.getenv("SECRETS_PROTOCOL_KEY");
    public static final String SECRETS_IV = System.getenv("SECRETS_PROTOCOL_IV");

    public static final String SECRETS_PROXY_KEY = System.getenv("SECRETS_PROXY_KEY");
    private static final String INTERNAL_ERROR = "Internal error.";

    public static final String SUCCESSFUL_CREATION = "Successful creation.";
    public static final String EMPTY_UPLOAD = "No content to upload.";
    public static final String SUCCESSFUL_DELETION = "Successful deletion.";
    private static final String BAD_NODE = "Data must only contain concrete nodes: IRI, Blank, Literal.";
    private static final String NOT_IMPLEMENTED = "Not implemented.";
    private static final String DELETE_ERROR_PREFIX = "Failure to delete triplestore. Error occurred while saving secrets: ";

    private static final QueryEngine queryEngine = new SPARQLQueryEngine(new DefaultSPARQLPlanner());

    @Override
    public Response create(Cookie cookie, EncryptedCreateForm form) {
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            String triplestoreID = form.getTriplestoreID();
            String issuer = form.getIssuer();
            try (CloseableHttpResponse response = IAMClient.createTriplestore(httpClient, cookie, triplestoreID, issuer)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HTTPUtils.buildResponse(response);
            }

            String accessToken;
            try (CloseableHttpResponse response = IAMClient.createAccessToken(httpClient, cookie, issuer, triplestoreID)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HTTPUtils.buildResponse(response);
                accessToken = HTTPUtils.consumeResponseEntity(response);
            }
            try (CloseableHttpResponse response = IAMClient.acquireTriplestoreLock(httpClient, cookie, triplestoreID, accessToken)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode()) {
                    Response errorResponse = HTTPUtils.buildResponse(response);
                    try (CloseableHttpResponse ignore = IAMClient.deleteAccessToken(httpClient, cookie, triplestoreID, accessToken)) {
                        return errorResponse;
                    }
                }
            }
            if (form.getContents() == null)
                return Response.ok(SUCCESSFUL_CREATION).build();
            List<Triple> triples = parseTriples(form.getContents(), parseRDFLanguage(form.getSyntax()));
            if (triples.isEmpty())
                return Response.ok(SUCCESSFUL_CREATION).build();

            List<String> expirableAccessTokens;
            try (CloseableHttpResponse response = IAMClient.createExpirableAccessTokens(httpClient, cookie, triplestoreID, accessToken, 2)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HTTPUtils.buildResponse(response);
                expirableAccessTokens = ParsingUtils.parseListOfStrings(HTTPUtils.consumeResponseEntity(response));
            }

            switch (form.getProtocolVersion()) {
                case V1 -> {
                    Protocol1 p = new Protocol1(triplestoreID);
                    try (CloseableHttpResponse response = VaultClient.saveProtocolSecrets(httpClient, triplestoreID,
                            generateSecretsMap(p), expirableAccessTokens.get(0))) {
                        if (response.getStatusLine().getStatusCode() != OK.getStatusCode()) {
                            Response errorResponse = HTTPUtils.buildResponse(response);
                            try (CloseableHttpResponse response2 = IAMClient.deleteTriplestore(httpClient, cookie, triplestoreID, accessToken)) {
                                if (response2.getStatusLine().getStatusCode() != OK.getStatusCode())
                                    errorResponse = HTTPUtils.buildResponse(DELETE_ERROR_PREFIX, response2);
                            }
                            try (CloseableHttpResponse ignored = IAMClient.releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                                 CloseableHttpResponse ignored2 = IAMClient.deleteAccessToken(httpClient, cookie, triplestoreID, accessToken)) {
                                return errorResponse;
                            }
                        }
                    }

                    Collections.shuffle(triples);
                    p.exec(triples);

                    try (CloseableHttpResponse response = EncryptedTriplestoreClient.upload(httpClient, triplestoreID,
                            p.getEncryptedT(), expirableAccessTokens.get(1));
                         CloseableHttpResponse ignored = IAMClient.releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                         CloseableHttpResponse ignored2 = IAMClient.deleteAccessToken(httpClient, cookie, triplestoreID, accessToken)) {
                        return HTTPUtils.buildResponse(response);
                    }
                }
                case V2 -> {
                    //TODO: Create protocol v2
                    return Response.ok(NOT_IMPLEMENTED).status(Response.Status.NOT_IMPLEMENTED).build();
                }
                default -> throw new IllegalStateException("Unexpected value: " + form.getProtocolVersion());
            }
        } catch (UnknownRDFLanguageException e) {
            return Response.ok(INVALID_SYNTAX).status(Response.Status.BAD_REQUEST).build();
        } catch (InvalidNodeException e) {
            return Response.ok(BAD_NODE).status(Response.Status.BAD_REQUEST).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response delete(Cookie cookie, String triplestoreID, String issuer) {
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            String accessToken;
            try (CloseableHttpResponse response = IAMClient.createAccessToken(httpClient, cookie, issuer, triplestoreID)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HTTPUtils.buildResponse(response);
                accessToken = HTTPUtils.consumeResponseEntity(response);
            }

            try (CloseableHttpResponse response = IAMClient.acquireTriplestoreLock(httpClient, cookie, triplestoreID, accessToken)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode()) {
                    Response errorResponse = HTTPUtils.buildResponse(response);
                    try (CloseableHttpResponse ignore = IAMClient.deleteAccessToken(httpClient, cookie, triplestoreID, accessToken)) {
                        return errorResponse;
                    }
                }
            }

            List<String> expirableAccessTokens;
            try (CloseableHttpResponse response = IAMClient.createExpirableAccessTokens(httpClient, cookie, triplestoreID, accessToken, 2)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HTTPUtils.buildResponse(response);
                expirableAccessTokens = ParsingUtils.parseListOfStrings(HTTPUtils.consumeResponseEntity(response));
            }

            try (CloseableHttpResponse response = EncryptedTriplestoreClient.deleteAll(httpClient, triplestoreID, expirableAccessTokens.get(0))) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode()) {
                    Response errorResponse = HTTPUtils.buildResponse(response);
                    try (CloseableHttpResponse ignored = IAMClient.releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                         CloseableHttpResponse ignored2 = IAMClient.deleteAccessToken(httpClient, cookie, triplestoreID, accessToken)) {
                        return errorResponse;
                    }
                }
            }
            try (CloseableHttpResponse response = VaultClient.deleteProtocolSecrets(httpClient, triplestoreID, expirableAccessTokens.get(1))) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode()) {
                    Response errorResponse = HTTPUtils.buildResponse(response);
                    try (CloseableHttpResponse ignored = IAMClient.releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                         CloseableHttpResponse ignored2 = IAMClient.deleteAccessToken(httpClient, cookie, triplestoreID, accessToken)) {
                        return errorResponse;
                    }
                }
            }
            try (CloseableHttpResponse response = IAMClient.deleteTriplestore(httpClient, cookie, triplestoreID, accessToken)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode()) {
                    Response errorResponse = HTTPUtils.buildResponse(response);
                    try (CloseableHttpResponse ignored = IAMClient.releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                         CloseableHttpResponse ignored2 = IAMClient.deleteAccessToken(httpClient, cookie, triplestoreID, accessToken)) {
                        return errorResponse;
                    }
                }

            }
            return Response.ok(SUCCESSFUL_DELETION).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response upload(Cookie cookie, UploadForm form) {
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            String issuer = form.getIssuer();
            String triplestoreID = form.getTriplestoreID();

            String accessToken;
            try (CloseableHttpResponse response = IAMClient.createAccessToken(httpClient, cookie, issuer, triplestoreID)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HTTPUtils.buildResponse(response);
                accessToken = HTTPUtils.consumeResponseEntity(response);
            }


            try (CloseableHttpResponse response = IAMClient.acquireTriplestoreLock(httpClient, cookie, triplestoreID, accessToken)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode()) {
                    Response errorResponse = HTTPUtils.buildResponse(response);
                    try (CloseableHttpResponse ignore = IAMClient.deleteAccessToken(httpClient, cookie, triplestoreID, accessToken)) {
                        return errorResponse;
                    }
                }
            }
            List<String> expirableAccessTokens;
            try (CloseableHttpResponse response = IAMClient.createExpirableAccessTokens(httpClient, cookie, triplestoreID, accessToken, 3)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HTTPUtils.buildResponse(response);
                expirableAccessTokens = ParsingUtils.parseListOfStrings(HTTPUtils.consumeResponseEntity(response));
            }
            Map<String, String> secrets;
            try (CloseableHttpResponse response = VaultClient.getProtocolSecrets(httpClient, triplestoreID, expirableAccessTokens.get(1))) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode()) {
                    Response errorResponse = HTTPUtils.buildResponse(response);
                    try (CloseableHttpResponse ignore = IAMClient.deleteAccessToken(httpClient, cookie, triplestoreID, accessToken)) {
                        return errorResponse;
                    }
                }
                secrets = ParsingUtils.parseMapOfStringString(response.getEntity().toString());
            }

            List<Triple> triples = parseTriples(form.getContents(), parseRDFLanguage(form.getSyntax()));
            switch (ProtocolVersion.fromString(secrets.get(SECRETS_VERSION))) {
                case V1 -> {
                    Protocol1 p = initProtocol1(triplestoreID, secrets);
                    Collections.shuffle(triples);
                    try (Response response = fetchAndUpdateKeywords(httpClient, triplestoreID,
                            p.generateKeywordTrapdoorMap(triples), p, expirableAccessTokens.get(2))) {
                        if (response != null) {
                            try (CloseableHttpResponse ignored = IAMClient.releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                                 CloseableHttpResponse ignored2 = IAMClient.deleteAccessToken(httpClient, cookie, triplestoreID, accessToken)) {
                                return response;
                            }
                        }
                    }
                    Collections.shuffle(triples);
                    p.exec(triples);
                    try (CloseableHttpResponse response = EncryptedTriplestoreClient.upload(httpClient, triplestoreID,
                            p.getEncryptedT(), expirableAccessTokens.get(3));
                         CloseableHttpResponse ignored = IAMClient.releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                         CloseableHttpResponse ignored2 = IAMClient.deleteAccessToken(httpClient, cookie, triplestoreID, accessToken)) {
                        return HTTPUtils.buildResponse(response);
                    }
                }
                case V2 -> {
                    //TODO: Create protocol v2
                    return Response.ok(NOT_IMPLEMENTED).status(Response.Status.NOT_IMPLEMENTED).build();
                }

                default ->
                        throw new IllegalStateException("Unexpected value: " + ProtocolVersion.fromString(secrets.get(SECRETS_VERSION)));
            }
        } catch (UnknownRDFLanguageException e) {
            return Response.ok(INVALID_SYNTAX).status(Response.Status.BAD_REQUEST).build();
        } catch (InvalidNodeException e) {
            return Response.ok(BAD_NODE).status(Response.Status.BAD_REQUEST).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response answerSPARQLQuery(Cookie cookie, QueryForm form) {
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            String accessToken;
            String triplestoreID = form.getTriplestoreID();
            try (CloseableHttpResponse response = IAMClient.createAccessToken(httpClient, cookie, form.getIssuer(), triplestoreID)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HTTPUtils.buildResponse(response);
                accessToken = HTTPUtils.consumeResponseEntity(response);
            }
            DefaultQueryExecutionPlan plan = (DefaultQueryExecutionPlan) queryEngine.getQueryPlan(form.getQuery());


            Map<String, Set<Var>> searchJobVars = new HashMap<>();

            plan.getJobs().forEach(
                    (id, j) -> {
                        if (j instanceof SearchJob)
                            searchJobVars.put(id, Utils.extractVars((SearchJob) j));
                    }
            );
            int numExpireTokens = 3 + (searchJobVars.size() * 2);
            List<String> expirableAccessTokens;
            try (CloseableHttpResponse response = IAMClient.createExpirableAccessTokens(httpClient, cookie, triplestoreID, accessToken, numExpireTokens)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HTTPUtils.buildResponse(response);
                expirableAccessTokens = ParsingUtils.parseListOfStrings(HTTPUtils.consumeResponseEntity(response));
            }

            Map<String, String> secrets;
            try (CloseableHttpResponse response = VaultClient.getProtocolSecrets(httpClient, triplestoreID, expirableAccessTokens.get(numExpireTokens))) {
                numExpireTokens--;
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode()) {
                    Response errorResponse = HTTPUtils.buildResponse(response);
                    try (CloseableHttpResponse ignore = IAMClient.deleteAccessToken(httpClient, cookie, triplestoreID, accessToken)) {
                        return errorResponse;
                    }
                }
                secrets = ParsingUtils.parseMapOfStringString(response.getEntity().toString());
            }

            switch (ProtocolVersion.fromString(secrets.get(SECRETS_VERSION))) {
                case V1 -> {
                    Protocol1 p = initProtocol1(triplestoreID, secrets);
                    //TODO: Generate trapdoor to get keyword info

                    for (String searchJobID : searchJobVars.keySet()) {
                        //Generate prepare requests
                        try (CloseableHttpResponse response =
                                     EncryptedTriplestoreController.prepareBinding(httpClient, triplestoreID, expirableAccessTokens.get(numExpireTokens))) {
                            numExpireTokens--;
                            if (response.getStatusLine().getStatusCode() != OK.getStatusCode()) {
                                Response errorResponse = HTTPUtils.buildResponse(response);
                                try (CloseableHttpResponse ignore = IAMClient.deleteAccessToken(httpClient, cookie, triplestoreID, accessToken)) {
                                    return errorResponse;
                                }
                            }
                        }
                    }


                    //TODO: prepare search jobs
                    plan.getJobs().replaceAll(
                            (id, j) -> {
                                if (j instanceof SearchJob) {
                                    return new SecureSearchJob(id, Utils.extractVars((SearchJob) j));
                                }
                                return j;
                            }
                    );

                    try (CloseableHttpResponse response = ProxyClient.query(httpClient, p.getK2(), plan);
                         ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                        List<Var> vars = plan.getVars();
                        SPARQLResult sparqlResult = ParsingUtils.parseSPARQLResult(HTTPUtils.consumeResponseEntity(response));
                        Collection<Binding> bindings = orderResultsIfNeeded(sparqlResult);

                        ResultSetFormatter.outputAsJSON(out, ResultSetStream.create(vars, bindings.iterator()));
                        CloseableHttpResponse ignored = IAMClient.deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                        return HTTPUtils.buildResponse(response);
                    }
                }
                case V2 -> {
                    //TODO: Create protocol v2
                    return Response.ok(NOT_IMPLEMENTED).status(Response.Status.NOT_IMPLEMENTED).build();
                }
                default ->
                        throw new IllegalStateException("Unexpected value: " + ProtocolVersion.fromString(secrets.get(SECRETS_VERSION)));
            }
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    private Collection<Binding> orderResultsIfNeeded(SPARQLResult sparqlResult) {
        if (sparqlResult.isOrdered()) {
            if (sparqlResult.isDistinct()) {
                Collection<Binding> bindings = new TreeSet<>(new BindingComparator(sparqlResult.getSortConditions()));
                bindings.addAll(sparqlResult.getBindings());
                return bindings;
            } else
                return sparqlResult.getBindings()
                        .stream().sorted(new BindingComparator(sparqlResult.getSortConditions()))
                        .collect(Collectors.toList());
        }
        return sparqlResult.getBindings();
    }


    private Response fetchAndUpdateKeywords(HttpClient httpClient, String triplestoreID, Map<String, String> keywordTrapdoorMap, Protocol1 protocol, String accessToken) throws InvalidNodeException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, IOException, InvalidKeyException {
        List<String> trapdoors = new ArrayList<>(keywordTrapdoorMap.size());
        List<String> keywords = new ArrayList<>(keywordTrapdoorMap.size());
        int i = 0;
        for (Map.Entry<String, String> entry : keywordTrapdoorMap.entrySet()) {
            trapdoors.add(entry.getKey());
            keywords.add(i, entry.getValue());
            i++;
        }

        List<String> keywordsTotals;
        try (CloseableHttpResponse response = EncryptedTriplestoreClient.search(httpClient, triplestoreID, trapdoors, accessToken)) {
            if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                return HTTPUtils.buildResponse(response);
            keywordsTotals = ParsingUtils.parseListOfStrings(EntityUtils.toString(response.getEntity()));
        }
        protocol.updateKeywords(protocol.generateKeywordIVMap(keywords, keywordsTotals));
        return null;
    }

}
