package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.graph.Triple;
import pt.fct.nova.id.srv.application.SPARQLQueryEngine;
import pt.fct.nova.id.srv.application.clients.*;
import pt.fct.nova.id.srv.application.protocols.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.query.plans.DefaultQueryExecutionPlan;
import pt.fct.nova.id.srv.application.query.plans.DefaultSPARQLPlanner;
import pt.fct.nova.id.srv.presentation.api.TriplestoreAPI;
import pt.fct.nova.id.srv.presentation.api.dtos.QueryForm;
import pt.fct.nova.id.srv.presentation.api.dtos.UploadForm;
import pt.fct.nova.id.srv.presentation.exceptions.UnknownRDFLanguageException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;

import static jakarta.ws.rs.core.Response.Status.*;
import static pt.fct.nova.id.srv.presentation.controllers.EncryptedTriplestoreController.*;
import static pt.fct.nova.id.srv.presentation.controllers.ParsingUtils.*;


@Path("triplestores")
public class TriplestoreController implements TriplestoreAPI {
    public static final String INVALID_SYNTAX = "Invalid syntax.";
    private static final String BAD_NODE = "Data must only contain concrete nodes: IRI, Blank, Literal.";
    private static final String NOT_IMPLEMENTED_ERROR = "Operation not yet supported.";
    private static final SPARQLQueryEngine queryEngine = new SPARQLQueryEngine(new DefaultSPARQLPlanner());

    @Override
    public Response create(Cookie cookie, UploadForm form) {
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            String triplestoreID = form.getTriplestoreID();
            String issuer = form.getIssuer();

            HTTPResponse response = createTriplestoreAccessPolicy(httpClient, cookie, triplestoreID, issuer);
            if (response.getStatus() != OK)
                return response.build();
            if (form.getContents() == null)
                return Response.ok(SUCCESSFUL_CREATION).build();

            List<Triple> triples = parseTriples(form.getContents(), parseRDFLanguage(form.getSyntax()));
            if (triples.isEmpty())
                return Response.ok(SUCCESSFUL_CREATION).build();

            response = createAccessToken(httpClient, cookie, issuer, triplestoreID);
            if (response.getStatus() != OK)
                return response.build();
            String accessToken = response.getBody();

            return upload(httpClient, cookie, triplestoreID, triples, accessToken);
        } catch (UnknownRDFLanguageException e) {
            return Response.ok(INVALID_SYNTAX).status(Response.Status.BAD_REQUEST).build();
        } catch (InvalidNodeException e) {
            return Response.ok(BAD_NODE).status(Response.Status.BAD_REQUEST).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response listTriplestores(Cookie cookie, String issuer, boolean write, boolean read, boolean owns) {
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            return listTriplestores(httpClient, cookie, issuer, write, read, owns).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response upload(Cookie cookie, UploadForm form) {
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            String triplestoreID = form.getTriplestoreID();
            InputStream contents = form.getContents();
            if (contents == null)
                return Response.ok(EMPTY_UPLOAD).status(Response.Status.BAD_REQUEST).build();
            List<Triple> triples = parseTriples(contents, parseRDFLanguage(form.getSyntax()));
            if (!triples.isEmpty()) {
                HTTPResponse response = createAccessToken(httpClient, cookie, form.getIssuer(), triplestoreID);
                if (response.getStatus() != OK)
                    return response.build();
                return upload(httpClient, cookie, triplestoreID, triples, response.getBody());
            }
            return Response.ok(EMPTY_UPLOAD).status(Response.Status.BAD_REQUEST).build();
        } catch (UnknownRDFLanguageException e) {
            return Response.ok(INVALID_SYNTAX).status(Response.Status.BAD_REQUEST).build();
        } catch (InvalidNodeException e) {
            return Response.ok(BAD_NODE).status(Response.Status.BAD_REQUEST).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    private Response upload(HttpClient httpClient, Cookie cookie, String triplestoreID, List<Triple> triples, String accessToken) throws IOException, InvalidNodeException, URISyntaxException {
        HTTPResponse response = acquireTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
        if (response.getStatus() != OK)
            return deleteAccessToken(httpClient, cookie, triplestoreID, accessToken).build();

        response = createExpirableTokens(httpClient, cookie, triplestoreID, accessToken, 1);
        if (response.getStatus() != OK)
            return response.build();
        List<String> expirableAccessTokens = ParsingUtils.parseListOfStrings(response.getBody());

        response = upload(httpClient, triplestoreID, triples, expirableAccessTokens.get(0));
        releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
        deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);

        return response.build();
    }


    @Override
    public Response delete(Cookie cookie, String triplestoreID, String issuer) {
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            HTTPResponse response = createAccessToken(httpClient, cookie, issuer, triplestoreID);
            if (response.getStatus() != OK)
                return response.build();
            String accessToken = response.getBody();

            response = acquireTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
            if (response.getStatus() != OK)
                return deleteAccessToken(httpClient, cookie, triplestoreID, accessToken).build();

            response = createExpirableTokens(httpClient, cookie, triplestoreID, accessToken, 1);
            if (response.getStatus() != OK)
                return response.build();
            List<String> expirableAccessTokens = ParsingUtils.parseListOfStrings(response.getBody());

            response = deleteTriplestoreContents(httpClient, triplestoreID, expirableAccessTokens.get(0));
            if (response.getStatus() != OK) {
                releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
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
    public Response answerSPARQLQuery(Cookie cookie, QueryForm form) {
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            String triplestoreID = form.getTriplestoreID();
            String issuer = form.getIssuer();

            HTTPResponse response = createAccessToken(httpClient, cookie, issuer, triplestoreID);
            if (response.getStatus() != OK)
                return response.build();
            String accessToken = response.getBody();

            response = createExpirableTokens(httpClient, cookie, triplestoreID, accessToken, 1);
            if (response.getStatus() != OK)
                return response.build();
            List<String> expirableAccessTokens = ParsingUtils.parseListOfStrings(response.getBody());

            DefaultQueryExecutionPlan plan = (DefaultQueryExecutionPlan) queryEngine.getQueryPlan(form.getQuery());
            response = sendSPARQLQuery(httpClient, triplestoreID, plan, expirableAccessTokens.get(0));
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return response.build();
        } catch (NotImplemented e) {
            return Response.ok(NOT_IMPLEMENTED_ERROR).status(INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response updateTriplestoreOwner(Cookie cookie, String triplestoreID, String issuer, String target) {
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            HTTPResponse response = createAccessToken(httpClient, cookie, issuer, triplestoreID);
            if (response.getStatus() != OK)
                return response.build();
            String accessToken = response.getBody();

            response = acquireTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
            if (response.getStatus() != OK)
                return deleteAccessToken(httpClient, cookie, triplestoreID, accessToken).build();

            response = updateTriplestoreOwner(httpClient, cookie, triplestoreID, target, accessToken);
            releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return response.build();
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response issueAccessRequest(Cookie cookie, String triplestoreID, String issuer, boolean write) {
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()){
             return requestAccess(httpClient, cookie, triplestoreID, issuer, write).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response listPendingAccessRequests(Cookie cookie, String triplestoreID, String issuer) {
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            HTTPResponse response = createAccessToken(httpClient, cookie, issuer, triplestoreID);
            if (response.getStatus() != OK)
                return response.build();
            String accessToken = response.getBody();

            response = listPendingAccessRequests(httpClient, cookie, triplestoreID, accessToken);
            IAMClient.deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return response.build();
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }



    @Override
    public Response processPendingAccessRequest(Cookie cookie, String triplestoreID, String issuer, String requestID, boolean accept) {
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            HTTPResponse response = createAccessToken(httpClient, cookie, issuer, triplestoreID);
            if (response.getStatus() != OK)
                return response.build();
            String accessToken = response.getBody();
            response = processAccessRequest(httpClient, cookie, triplestoreID, requestID, accept, accessToken);
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return response.build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response grantAccess(Cookie cookie, String triplestoreID, String issuer, String target, boolean write) {
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            HTTPResponse response = createAccessToken(httpClient, cookie, issuer, triplestoreID);
            if (response.getStatus() != OK)
                return response.build();
            String accessToken = response.getBody();

            response = acquireTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
            if (response.getStatus() != OK)
                return deleteAccessToken(httpClient, cookie, triplestoreID, accessToken).build();

            response = grantAccess(httpClient, cookie, triplestoreID, target, write, accessToken);
            releaseTriplestoreLock(httpClient, cookie, issuer, triplestoreID);
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return response.build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response revokeAccess(Cookie cookie, String triplestoreID, String issuer, String target, boolean write) {
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            HTTPResponse response = createAccessToken(httpClient, cookie, issuer, triplestoreID);
            if (response.getStatus() != OK)
                return response.build();
            String accessToken = response.getBody();

            response = acquireTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
            if (response.getStatus() != OK)
                return deleteAccessToken(httpClient, cookie, triplestoreID, accessToken).build();

            response = revokeAccess(httpClient, cookie, triplestoreID, target, write, accessToken);
            releaseTriplestoreLock(httpClient, cookie, issuer, triplestoreID);
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return response.build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response listUsersWithAccess(Cookie cookie, String triplestoreID, String issuer, boolean write) {
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            HTTPResponse response = createAccessToken(httpClient, cookie, issuer, triplestoreID);
            if (response.getStatus() != OK)
                return response.build();
            String accessToken = response.getBody();

            response = listUsersWithAccess(httpClient, cookie, triplestoreID, write, accessToken);
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return response.build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    public static HTTPResponse createTriplestoreAccessPolicy(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID, String issuer) throws IOException {
        try (CloseableHttpResponse response = IAMClient.createTriplestore(httpClient, cookie, triplestoreID, issuer)) {
            return new HTTPResponse(response);
        }
    }


    public static HTTPResponse deleteTriplestoreAccessPolicy(HttpClient httpClient, Cookie cookie, String triplestoreID, String accessToken) throws IOException {
        try (CloseableHttpResponse response = IAMClient.deleteTriplestore(httpClient, cookie, triplestoreID, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    private HTTPResponse deleteTriplestoreContents(HttpClient httpClient, String triplestoreID, String expirableAccessToken) throws IOException {
        try (CloseableHttpResponse response = TriplestoreClient.deleteAll(httpClient, triplestoreID, expirableAccessToken)) {
            return new HTTPResponse(response);
        }
    }

    private HTTPResponse listTriplestores(CloseableHttpClient httpClient, Cookie cookie, String issuer, boolean write, boolean read, boolean owns) throws IOException, URISyntaxException {
        try (CloseableHttpResponse response = IAMClient.listTriplestores(httpClient, cookie, issuer, write, read, owns)) {
            return new HTTPResponse(response);
        }
    }

    public static HTTPResponse createAccessToken(CloseableHttpClient httpClient, Cookie cookie, String issuer, String triplestoreID) throws IOException {
        try (CloseableHttpResponse response = IAMClient.createAccessToken(httpClient, cookie, issuer, triplestoreID)) {
            return new HTTPResponse(response);
        }
    }

    public static HTTPResponse createExpirableTokens(HttpClient httpClient, Cookie cookie, String triplestoreID, String accessToken, int total) throws IOException, URISyntaxException {
        try (CloseableHttpResponse response = IAMClient.createExpirableAccessTokens(httpClient, cookie, triplestoreID, accessToken, total)) {
            return new HTTPResponse(response);
        }
    }

    public static HTTPResponse acquireTriplestoreLock(HttpClient httpClient, Cookie cookie, String triplestoreID, String accessToken) throws IOException {
        try (CloseableHttpResponse response = IAMClient.acquireTriplestoreLock(httpClient, cookie, triplestoreID, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    public static HTTPResponse releaseTriplestoreLock(HttpClient httpClient, Cookie cookie, String triplestoreID, String accessToken) throws IOException {
        try (CloseableHttpResponse response = IAMClient.releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    public static HTTPResponse deleteAccessToken(HttpClient httpClient, Cookie cookie, String triplestoreID, String accessToken) throws IOException {
        try (CloseableHttpResponse response = IAMClient.deleteAccessToken(httpClient, cookie, triplestoreID, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    private HTTPResponse upload(HttpClient httpClient, String triplestoreID, List<Triple> triples, String expirableAccessToken) throws IOException, InvalidNodeException {
        try (CloseableHttpResponse response = TriplestoreClient.upload(httpClient, triplestoreID, triples, expirableAccessToken)) {
            return new HTTPResponse(response);
        }
    }

    private HTTPResponse sendSPARQLQuery(CloseableHttpClient httpClient, String triplestoreID, DefaultQueryExecutionPlan plan, String expirableAccessToken) throws IOException {
        try (CloseableHttpResponse response = TriplestoreClient.query(httpClient, triplestoreID, plan, expirableAccessToken)) {
            return new HTTPResponse(response);
        }
    }

    private HTTPResponse updateTriplestoreOwner(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID, String target, String accessToken) throws IOException {
        try(CloseableHttpResponse response = IAMClient.updateTriplestoreOwner(httpClient, cookie, triplestoreID, target, accessToken)){
            return new HTTPResponse(response);
        }
    }

    private HTTPResponse revokeAccess(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID, String target, boolean write, String accessToken) throws IOException, URISyntaxException {
        try (CloseableHttpResponse response = IAMClient.revokeAccess(httpClient, cookie, triplestoreID, target, write, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    private HTTPResponse grantAccess(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID, String target, boolean write, String accessToken) throws IOException, URISyntaxException {
        try (CloseableHttpResponse response = IAMClient.grantAccess(httpClient, cookie, triplestoreID, target, write, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    private HTTPResponse listPendingAccessRequests(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID, String accessToken) throws IOException {
        try (CloseableHttpResponse response = IAMClient.listPendingAccessRequests(httpClient, cookie, triplestoreID, accessToken)){
            return new HTTPResponse(response);
        }
    }

    private HTTPResponse requestAccess(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID, String issuer, boolean write) throws IOException, URISyntaxException {
        try (CloseableHttpResponse response = IAMClient.requestAccess(httpClient, cookie, triplestoreID, issuer, write)){
            return new HTTPResponse(response);
        }
    }

    private HTTPResponse processAccessRequest(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID, String requestID, boolean accept, String accessToken) throws IOException, URISyntaxException {
        try (CloseableHttpResponse response = IAMClient.processAccessRequest(httpClient, cookie, triplestoreID, requestID, accept, accessToken)) {
            return new HTTPResponse(response);
        }
    }

    private HTTPResponse listUsersWithAccess(CloseableHttpClient httpClient, Cookie cookie, String triplestoreID, boolean write, String accessToken) throws URISyntaxException, IOException {
        try (CloseableHttpResponse response = IAMClient.listUsersWithAccess(httpClient, cookie, triplestoreID, write, accessToken)) {
            return new HTTPResponse(response);
        }
    }

}
