package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.graph.Triple;
import pt.fct.nova.id.srv.application.SPARQLQueryEngine;
import pt.fct.nova.id.srv.application.clients.HttpUtils;
import pt.fct.nova.id.srv.application.clients.IAMClient;
import pt.fct.nova.id.srv.application.clients.TriplestoreClient;
import pt.fct.nova.id.srv.application.protocols.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.query.plans.SimpleQueryExecutionPlan;
import pt.fct.nova.id.srv.application.query.plans.SimpleSPARQLPlanner;
import pt.fct.nova.id.srv.presentation.api.TriplestoreAPI;
import pt.fct.nova.id.srv.presentation.api.dtos.QueryForm;
import pt.fct.nova.id.srv.presentation.api.dtos.UploadForm;
import pt.fct.nova.id.srv.presentation.exceptions.UnknownRDFLanguageException;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static jakarta.ws.rs.core.Response.Status.*;
import static pt.fct.nova.id.srv.presentation.controllers.ClientUtils.*;
import static pt.fct.nova.id.srv.presentation.controllers.EncryptedTriplestoreController.SUCCESSFUL_DELETION;


@Path("triplestores")
public class TriplestoreController implements TriplestoreAPI {
    public static final String INVALID_SYNTAX = "Invalid syntax.";
    private static final String BAD_NODE = "Data must only contain concrete nodes: IRI, Blank, Literal.";

    private static final String NOT_IMPLEMENTED_ERROR = "Operation not yet supported.";
    private static final SPARQLQueryEngine queryEngine = new SPARQLQueryEngine(new SimpleSPARQLPlanner());


    @Override
    public Response create(Cookie cookie, UploadForm form) {
        try {
            String triplestoreID = form.getTriplestoreID();
            String issuer = form.getIssuer();
            try (CloseableHttpResponse r = IAMClient.createTriplestore(cookie, triplestoreID, issuer)) {
                if (r.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(r);
            }

            String accessToken;
            try (CloseableHttpResponse r = IAMClient.createAccessToken(cookie, issuer, triplestoreID)) {
                if (r.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(r);
                accessToken = new String(r.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
            }
            return upload(cookie, triplestoreID, parseTriples(form.getContents(), parseRDFLanguage(form.getSyntax())), accessToken);
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        } catch (UnknownRDFLanguageException e) {
            return Response.ok(INVALID_SYNTAX).status(Response.Status.BAD_REQUEST).build();
        } catch (InvalidNodeException e) {
            return Response.ok(BAD_NODE).status(Response.Status.BAD_REQUEST).build();
        }
    }

    @Override
    public Response listTriplestores(Cookie cookie, String issuer, boolean write, boolean read, boolean owns) {
        try (CloseableHttpResponse response = IAMClient.listTriplestores(cookie, issuer, write, read, owns)) {
            return HttpUtils.buildResponse(response);
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response upload(Cookie cookie, UploadForm form) {
        try {
            String triplestoreID = form.getTriplestoreID();
            String accessToken;
            try (CloseableHttpResponse r = IAMClient.createAccessToken(cookie, form.getIssuer(), triplestoreID)) {
                if (r.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(r);
                accessToken = new String(r.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
            }
            return upload(cookie, triplestoreID, parseTriples(form.getContents(), parseRDFLanguage(form.getSyntax())), accessToken);
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        } catch (UnknownRDFLanguageException e) {
            return Response.ok(INVALID_SYNTAX).status(Response.Status.BAD_REQUEST).build();
        } catch (InvalidNodeException e) {
            return Response.ok(BAD_NODE).status(Response.Status.BAD_REQUEST).build();
        }
    }

    private Response upload(Cookie cookie, String triplestoreID, List<Triple> triples, String accessToken) throws IOException, InvalidNodeException {
        try (CloseableHttpResponse r = IAMClient.acquireTriplestoreLock(cookie, triplestoreID, accessToken)) {
            if (r.getStatusLine().getStatusCode() != OK.getStatusCode()) {
                IAMClient.deleteAccessToken(cookie, triplestoreID, accessToken);
                return HttpUtils.buildResponse(r);
            }
        }
        try (CloseableHttpResponse r = TriplestoreClient.upload(cookie, triplestoreID, triples, accessToken)) {
            IAMClient.releaseTriplestoreLock(cookie, triplestoreID, accessToken);
            IAMClient.deleteAccessToken(cookie, triplestoreID, accessToken);
            return HttpUtils.buildResponse(r);
        }
    }

    @Override
    public Response delete(Cookie cookie, String triplestoreID, String issuer) {
        try {
            String accessToken;
            try (CloseableHttpResponse response = IAMClient.createAccessToken(cookie, issuer, triplestoreID)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
                accessToken = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
            }

            try (CloseableHttpResponse response = IAMClient.acquireTriplestoreLock(cookie, triplestoreID, accessToken)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode()) {
                    IAMClient.deleteAccessToken(cookie, triplestoreID, accessToken);
                    return HttpUtils.buildResponse(response);
                }
            }

            try (CloseableHttpResponse response = TriplestoreClient.deleteAll(cookie, triplestoreID, accessToken)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode()) {
                    IAMClient.releaseTriplestoreLock(cookie, triplestoreID, accessToken);
                    IAMClient.deleteAccessToken(cookie, triplestoreID, accessToken);
                    return HttpUtils.buildResponse(response);
                }
            }
            try (CloseableHttpResponse response = IAMClient.deleteTriplestore(cookie, triplestoreID, accessToken)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode()) {
                    IAMClient.releaseTriplestoreLock(cookie, triplestoreID, accessToken);
                    IAMClient.deleteAccessToken(cookie, triplestoreID, accessToken);
                    return HttpUtils.buildResponse(response);
                }
            }
            return Response.ok(SUCCESSFUL_DELETION).build();
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response answerSPARQLQuery(Cookie cookie, QueryForm form) {
        try {
            String accessToken;
            String triplestoreID = form.getTriplestoreID();
            try (CloseableHttpResponse response = IAMClient.createAccessToken(cookie, form.getIssuer(), triplestoreID)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
                accessToken = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
            }
            SimpleQueryExecutionPlan plan = (SimpleQueryExecutionPlan) queryEngine.getQueryPlan(form.getQuery());
            System.out.println("Execution order:" + Arrays.toString(plan.getExecutionOrder().toArray()));
            System.out.println("Vars:" + Arrays.toString(plan.getVars().toArray()));
            try (CloseableHttpResponse r = TriplestoreClient.query(cookie, triplestoreID, plan , accessToken)) {
                IAMClient.deleteAccessToken(cookie, triplestoreID, accessToken);
                return HttpUtils.buildResponse(r);
            }
        }catch (NotImplemented e) {
            return Response.ok(NOT_IMPLEMENTED_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
        catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response updateTriplestoreOwner(Cookie cookie, String triplestoreID, String issuer, String target) {
        try {
            String accessToken;
            try (CloseableHttpResponse response = IAMClient.createAccessToken(cookie, issuer, triplestoreID)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
                accessToken = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
            }

            try (CloseableHttpResponse response = IAMClient.acquireTriplestoreLock(cookie, triplestoreID, accessToken)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
            }
            try (CloseableHttpResponse response = IAMClient.updateTriplestoreOwner(cookie, triplestoreID, target, accessToken)) {
                IAMClient.releaseTriplestoreLock(cookie, triplestoreID, accessToken);
                IAMClient.deleteAccessToken(cookie, triplestoreID, accessToken);
                return HttpUtils.buildResponse(response);
            }
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response issueAccessRequest(Cookie cookie, String triplestoreID, String issuer, boolean write) {
        try (CloseableHttpResponse response = IAMClient.requestAccess(cookie, triplestoreID, issuer, write)) {
            return HttpUtils.buildResponse(response);
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response listPendingAccessRequests(Cookie cookie, String triplestoreID, String issuer) {
        try {
            String accessToken;
            try (CloseableHttpResponse response = IAMClient.createAccessToken(cookie, issuer, triplestoreID)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
                accessToken = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
            }

            try (CloseableHttpResponse response = IAMClient.listPendingAccessRequests(cookie, triplestoreID, accessToken)) {
                IAMClient.deleteAccessToken(cookie, triplestoreID, accessToken);
                return HttpUtils.buildResponse(response);
            }
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response processPendingAccessRequest(Cookie cookie, String triplestoreID, String issuer, String requestID, boolean accept) {
        try {
            String accessToken;
            try (CloseableHttpResponse response = IAMClient.createAccessToken(cookie, issuer, triplestoreID)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
                accessToken = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
            }

            try (CloseableHttpResponse response = IAMClient.processAccessRequest(cookie, triplestoreID, requestID, accept, accessToken)) {
                IAMClient.deleteAccessToken(cookie, triplestoreID, accessToken);
                return HttpUtils.buildResponse(response);
            }
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }


    @Override
    public Response grantAccess(Cookie cookie, String triplestoreID, String issuer, String target, boolean write) {
        try {
            String accessToken;
            try (CloseableHttpResponse response = IAMClient.createAccessToken(cookie, issuer, triplestoreID)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
                accessToken = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
            }

            try (CloseableHttpResponse response = IAMClient.acquireTriplestoreLock(cookie, triplestoreID, accessToken)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
            }

            try (CloseableHttpResponse response = IAMClient.grantAccess(cookie, triplestoreID, target, write, accessToken)) {
                IAMClient.releaseTriplestoreLock(cookie, triplestoreID, accessToken);
                IAMClient.deleteAccessToken(cookie, triplestoreID, accessToken);
                return HttpUtils.buildResponse(response);
            }
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response revokeAccess(Cookie cookie, String triplestoreID, String issuer, String target, boolean write) {
        try {
            String accessToken;
            try (CloseableHttpResponse response = IAMClient.createAccessToken(cookie, issuer, triplestoreID)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
                accessToken = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
            }

            try (CloseableHttpResponse response = IAMClient.acquireTriplestoreLock(cookie, triplestoreID, accessToken)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
            }

            try (CloseableHttpResponse response = IAMClient.revokeAccess(cookie, triplestoreID, target, write, accessToken)) {
                IAMClient.releaseTriplestoreLock(cookie, issuer, triplestoreID);
                IAMClient.deleteAccessToken(cookie, triplestoreID, accessToken);
                return HttpUtils.buildResponse(response);
            }
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response listUsersWithAccess(Cookie cookie, String triplestoreID, String issuer, boolean write) {
        try {
            String accessToken;
            try (CloseableHttpResponse response = IAMClient.createAccessToken(cookie, issuer, triplestoreID)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
                accessToken = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
            }

            try (CloseableHttpResponse response = IAMClient.listUsersWithAccess(cookie, triplestoreID, write, accessToken)) {
                IAMClient.deleteAccessToken(cookie, triplestoreID, accessToken);
                return HttpUtils.buildResponse(response);
            }
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }


}
