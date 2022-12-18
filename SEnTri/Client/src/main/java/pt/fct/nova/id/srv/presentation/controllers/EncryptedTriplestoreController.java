package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
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
import javax.crypto.SecretKey;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

import static jakarta.ws.rs.core.Response.Status.*;
import static pt.fct.nova.id.srv.presentation.controllers.ParsingUtils.*;
import static pt.fct.nova.id.srv.presentation.controllers.TriplestoreController.*;

@Path("triplestores/encrypted")
public class EncryptedTriplestoreController implements EncryptedTriplestoreAPI {
    public static final String SECRETS_VERSION = System.getenv("SECRETS_PROTOCOL_VERSION");
    public static final String SECRETS_KEY = System.getenv("SECRETS_PROTOCOL_KEY");
    public static final String SECRETS_IV = System.getenv("SECRETS_PROTOCOL_IV");
    private static final String INTERNAL_ERROR = "Internal error.";
    public static final String SUCCESSFUL_CREATION = "Successful creation.";
    public static final String EMPTY_UPLOAD = "No content to upload.";
    public static final String SUCCESSFUL_DELETION = "Successful deletion.";
    private static final String BAD_NODE = "Data must only contain concrete nodes: IRI, Blank, Literal.";
    private static final String NOT_IMPLEMENTED = "Not implemented.";

    private static final QueryEngine queryEngine = new SPARQLQueryEngine(new DefaultSPARQLPlanner());

    @Override
    public Response create(Cookie cookie, EncryptedCreateForm form) {
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            String triplestoreID = form.getTriplestoreID();
            String issuer = form.getIssuer();
            HTTPResponse response = createTriplestoreAccessPolicy(httpClient, cookie, triplestoreID, issuer);
            if (response.getStatus() != OK)
                return response.build();

            response = createAccessToken(httpClient, cookie, issuer, triplestoreID);
            if (response.getStatus() != OK)
                return response.build();
            String accessToken = response.getBody();

            response = acquireTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
            if (response.getStatus() != OK) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }

            if (form.getContents() == null)
                return Response.ok(SUCCESSFUL_CREATION).build();
            List<Triple> triples = parseTriples(form.getContents(), parseRDFLanguage(form.getSyntax()));

            switch (form.getProtocolVersion()) {
                case V1 -> {
                    Protocol1 p = new Protocol1(triplestoreID);

                    response = saveProtocolSecrets(httpClient, triplestoreID, generateSecretsMap(p), accessToken);
                    if (response.getStatus() != OK) {
                        HTTPResponse response2 = deleteTriplestoreAccessPolicy(httpClient, cookie, triplestoreID, accessToken);
                        if (response2.getStatus() != OK)
                            return response2.build();
                        releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                        deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                        return response.build();
                    }

                    Collections.shuffle(triples);
                    p.exec(triples);

                    response = uploadEncryptedTriplestoreContents(httpClient, triplestoreID, p.getEncryptedT(), accessToken);
                    releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                    deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                    return response.build();
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
            HTTPResponse response = createAccessToken(httpClient, cookie, issuer, triplestoreID);
            if (response.getStatus() != OK)
                return response.build();
            String accessToken = response.getBody();

            response = acquireTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
            if (response.getStatus() != OK) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }

            response = deleteEncryptedTriplestoreContents(httpClient, triplestoreID, accessToken);
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
                return response.build();
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

            HTTPResponse response = createAccessToken(httpClient, cookie, issuer, triplestoreID);
            if (response.getStatus() != OK)
                return response.build();
            String accessToken = response.getBody();

            response = acquireTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
            if (response.getStatus() != OK) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }

            if (form.getContents() == null)
                return Response.ok(EMPTY_UPLOAD).status(Response.Status.BAD_REQUEST).build();

            List<Triple> triples = parseTriples(form.getContents(), parseRDFLanguage(form.getSyntax()));
            if (!triples.isEmpty())
                return Response.ok(EMPTY_UPLOAD).status(Response.Status.BAD_REQUEST).build();

            response = getProtocolSecrets(httpClient, triplestoreID, accessToken);
            if (response.getStatus() != OK) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }
            Map<String, String> secrets = ParsingUtils.parseMapOfStringString(response.getBody());

            switch (ProtocolVersion.fromString(secrets.get(SECRETS_VERSION))) {
                case V1 -> {
                    Protocol1 protocol = initProtocol1(triplestoreID, secrets);
                    Collections.shuffle(triples);


                    response = fetchAndUpdateKeywords(httpClient, triplestoreID, protocol.generateKeywordTrapdoorMap(triples), protocol, accessToken);
                    if (response != null && response.getStatus() != OK) {
                        releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                        deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                    }

                    Collections.shuffle(triples);
                    protocol.exec(triples);

                    response = uploadEncryptedTriplestoreContents(httpClient, triplestoreID, protocol.getEncryptedT(), accessToken);
                    releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                    deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                    return response.build();
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
            String triplestoreID = form.getTriplestoreID();
            String issuer = form.getIssuer();

            HTTPResponse response = createAccessToken(httpClient, cookie, issuer, triplestoreID);
            if (response.getStatus() != OK)
                return response.build();
            String accessToken = response.getBody();

            DefaultQueryExecutionPlan plan = (DefaultQueryExecutionPlan) queryEngine.getQueryPlan(form.getQuery());

            Map<String, Set<Var>> searchJobVars = new HashMap<>();
            plan.getJobs().forEach(
                    (id, j) -> {
                        if (j instanceof SearchJob)
                            searchJobVars.put(id, Utils.extractVars((SearchJob) j));
                    }
            );

            response = getProtocolSecrets(httpClient, triplestoreID, accessToken);
            if (response.getStatus() != OK) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }
            Map<String, String> secrets = ParsingUtils.parseMapOfStringString(response.getBody());

            switch (ProtocolVersion.fromString(secrets.get(SECRETS_VERSION))) {
                case V1 -> {
                    Protocol1 protocol = initProtocol1(triplestoreID, secrets);
                    //TODO: Generate trapdoor to get keyword info

                    for (String searchJobID : searchJobVars.keySet()) {
                        //TODO: Check if binding is already prepared in proxy, update job else generate trapdoors
                        response = prepareSPARQLQueryBindings(httpClient, triplestoreID, null, accessToken);
                        if (response.getStatus() != OK)
                            return deleteAccessToken(httpClient, cookie, triplestoreID, accessToken).build();
                        //TODO: save results to aux map & update job
                    }

                    response = query(httpClient, protocol.getK2(), plan, accessToken);
                    if (response.getStatus() != OK) {
                        deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                        return response.build();
                    }
                    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                        List<Var> vars = plan.getVars();
                        SPARQLResult sparqlResult = ParsingUtils.parseSPARQLResult(response.getBody());
                        Collection<Binding> bindings = orderResultsIfNeeded(sparqlResult);
                        //TODO: Decrypt bindings & change to readable vars
                        ResultSetFormatter.outputAsJSON(out, ResultSetStream.create(vars, bindings.iterator()));
                        CloseableHttpResponse ignored = IAMClient.deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                        return Response.ok(out.toByteArray()).build();
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

    private HTTPResponse query(CloseableHttpClient httpClient, SecretKey secretKey, DefaultQueryExecutionPlan plan, String accessToken) throws IOException {
        try (CloseableHttpResponse response = ProxyClient.query(httpClient, secretKey, plan, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    private HTTPResponse prepareSPARQLQueryBindings(CloseableHttpClient httpClient, String triplestoreID, List<String> trapdoors, String accessToken) throws IOException {
        try (CloseableHttpResponse response = EncryptedTriplestoreClient.prepareSPARQLQueryBindings(httpClient, triplestoreID, trapdoors, accessToken)) {
            return new HTTPResponse(response);
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


    private HTTPResponse fetchAndUpdateKeywords(HttpClient httpClient, String triplestoreID, Map<String, String> keywordTrapdoorMap, Protocol1 protocol, String accessToken) throws InvalidNodeException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, IOException, InvalidKeyException {
        List<String> trapdoors = new ArrayList<>(keywordTrapdoorMap.size());
        List<String> keywords = new ArrayList<>(keywordTrapdoorMap.size());
        int i = 0;
        for (Map.Entry<String, String> entry : keywordTrapdoorMap.entrySet()) {
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

    private HTTPResponse searchEncryptedTriplestoreContents(HttpClient httpClient, String triplestoreID, List<String> trapdoors, String accessToken) throws IOException {
        try (CloseableHttpResponse response = EncryptedTriplestoreClient.search(httpClient, triplestoreID, trapdoors, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    private HTTPResponse getProtocolSecrets(CloseableHttpClient httpClient, String triplestoreID, String accessToken) throws IOException {
        try (CloseableHttpResponse response = VaultClient.getProtocolSecrets(httpClient, triplestoreID, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    private HTTPResponse uploadEncryptedTriplestoreContents(CloseableHttpClient httpClient, String triplestoreID, Map<String, String> encryptedT, String accessToken) throws IOException {
        try (CloseableHttpResponse response = EncryptedTriplestoreClient.upload(httpClient, triplestoreID, encryptedT, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    private HTTPResponse saveProtocolSecrets(CloseableHttpClient httpClient, String triplestoreID, Map<String, String> secrets, String accessToken) throws IOException {
        try (CloseableHttpResponse response = VaultClient.saveProtocolSecrets(httpClient, triplestoreID, secrets, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    private HTTPResponse deleteProtocolSecrets(CloseableHttpClient httpClient, String triplestoreID, String accessToken) throws IOException {
        try (CloseableHttpResponse response = VaultClient.deleteProtocolSecrets(httpClient, triplestoreID, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    private HTTPResponse deleteEncryptedTriplestoreContents(CloseableHttpClient httpClient, String triplestoreID, String accessToken) throws IOException {
        try (CloseableHttpResponse response = EncryptedTriplestoreClient.deleteAll(httpClient, triplestoreID, accessToken)) {
            return new HTTPResponse(response);
        }
    }

}
