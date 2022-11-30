package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.jena.graph.Triple;
import pt.fct.nova.id.srv.application.SPARQLQueryEngine;
import pt.fct.nova.id.srv.application.clients.HttpUtils;
import pt.fct.nova.id.srv.application.clients.IAMClient;
import pt.fct.nova.id.srv.application.clients.TriplestoreClient;
import pt.fct.nova.id.srv.application.query.plans.SimpleSPARQLPlanner;
import pt.fct.nova.id.srv.presentation.api.TriplestoreAPI;
import pt.fct.nova.id.srv.presentation.api.dtos.AccessForm;
import pt.fct.nova.id.srv.presentation.api.dtos.QueryForm;
import pt.fct.nova.id.srv.presentation.api.dtos.UploadForm;
import pt.fct.nova.id.srv.presentation.exceptions.UnknownRDFLanguageException;


import java.io.IOException;
import java.util.List;

import static jakarta.ws.rs.core.Response.Status.*;
import static pt.fct.nova.id.srv.presentation.controllers.ClientUtils.*;
import static pt.fct.nova.id.srv.presentation.controllers.SecureTriplestoreController.SUCCESSFUL_DELETION;


@Path("triplestore")
public class TriplestoreController implements TriplestoreAPI {
    public static final String INVALID_SYNTAX = "Invalid syntax.";

    private static final SPARQLQueryEngine queryEngine = new SPARQLQueryEngine(new SimpleSPARQLPlanner());


    @Override
    public Response create(Cookie cookie, UploadForm form) {
        try {
            String storeID = form.getStoreID();
            String issuer = form.getIssuer();
            try (CloseableHttpResponse r = IAMClient.createStore(cookie, storeID, issuer)) {
                if (r.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(r);
            }

            String accessToken;
            try (CloseableHttpResponse r = IAMClient.createAccessToken(cookie, issuer, storeID)) {
                if (r.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(r);
                accessToken = r.getEntity().toString();
            }
            return upload(cookie, storeID, parseTriples(form.getContents(), parseRDFLanguage(form.getSyntax())), accessToken);
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        } catch (UnknownRDFLanguageException e) {
            return Response.ok(INVALID_SYNTAX).status(Response.Status.BAD_REQUEST).build();
        }
    }

    @Override
    public Response listStores(Cookie cookie, String issuer, boolean write, boolean read, boolean owns) {
        try (CloseableHttpResponse response = IAMClient.listStores(cookie, issuer, write, read, owns)) {
            return HttpUtils.buildResponse(response);
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response upload(Cookie cookie, String storeID, UploadForm form) {
        try {
            String accessToken;
            try (CloseableHttpResponse r = IAMClient.createAccessToken(cookie, form.getIssuer(), storeID)) {
                if (r.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(r);
                accessToken = r.getEntity().toString();
            }
            return upload(cookie, storeID, parseTriples(form.getContents(), parseRDFLanguage(form.getSyntax())), accessToken);
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        } catch (UnknownRDFLanguageException e) {
            return Response.ok(INVALID_SYNTAX).status(Response.Status.BAD_REQUEST).build();
        }
    }

    private Response upload(Cookie cookie, String storeID, List<Triple> triples, String accessToken) throws IOException {
        try (CloseableHttpResponse r = IAMClient.acquireStoreLock(cookie, storeID, accessToken)) {
            if (r.getStatusLine().getStatusCode() != OK.getStatusCode()) {
                IAMClient.deleteAccessToken(cookie, accessToken);
                return HttpUtils.buildResponse(r);
            }
        }

        try (CloseableHttpResponse r = TriplestoreClient.upload(cookie, storeID, triples, accessToken)) {
            IAMClient.releaseStoreLock(cookie, storeID, accessToken);
            IAMClient.deleteAccessToken(cookie, accessToken);
            return HttpUtils.buildResponse(r);
        }
    }

    @Override
    public Response delete(Cookie cookie, String storeID, String username) {
        try {
            String accessToken;
            try (CloseableHttpResponse response = IAMClient.createAccessToken(cookie, username, storeID)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
                accessToken = response.getEntity().toString();
            }

            try (CloseableHttpResponse response = IAMClient.acquireStoreLock(cookie, storeID, accessToken)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode()) {
                    IAMClient.deleteAccessToken(cookie, accessToken);
                    return HttpUtils.buildResponse(response);
                }
            }

            try (CloseableHttpResponse response = TriplestoreClient.deleteAll(cookie, storeID, accessToken)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode()) {
                    IAMClient.releaseStoreLock(cookie, storeID, accessToken);
                    IAMClient.deleteAccessToken(cookie, accessToken);
                    return HttpUtils.buildResponse(response);
                }
            }
            try (CloseableHttpResponse response = IAMClient.deleteStore(cookie, storeID, accessToken)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode()) {
                    IAMClient.releaseStoreLock(cookie, storeID, accessToken);
                    IAMClient.deleteAccessToken(cookie, accessToken);
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
            String storeID = form.getStoreID();
            try (CloseableHttpResponse response = IAMClient.createAccessToken(cookie, form.getIssuer(), storeID)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
                accessToken = response.getEntity().toString();
            }
            try (CloseableHttpResponse r = TriplestoreClient.query(cookie, storeID, queryEngine.getQueryPlan(form.getQuery()), accessToken)) {
                IAMClient.deleteAccessToken(cookie, accessToken);
                return HttpUtils.buildResponse(r);
            }
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response updateTriplestoreOwner(Cookie cookie, String storeID, String issuer, String username) {
        try {
            String accessToken;
            try (CloseableHttpResponse response = IAMClient.createAccessToken(cookie, username, storeID)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
                accessToken = response.getEntity().toString();
            }

            try (CloseableHttpResponse response = IAMClient.acquireStoreLock(cookie, storeID, accessToken)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
            }
            try (CloseableHttpResponse response = IAMClient.updateStoreOwner(cookie, storeID, issuer, accessToken)) {
                IAMClient.releaseStoreLock(cookie, storeID, accessToken);
                IAMClient.deleteAccessToken(cookie, accessToken);
                return HttpUtils.buildResponse(response);
            }
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response issueAccessRequest(Cookie cookie, String storeID, AccessForm form) {
        try (CloseableHttpResponse response = IAMClient.requestAccess(cookie, storeID, form)) {
            return HttpUtils.buildResponse(response);
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }


    @Override
    public Response grantAccess(Cookie cookie, String storeID, String issuer, String username, boolean write) {
        try {
            String accessToken;
            try (CloseableHttpResponse response = IAMClient.createAccessToken(cookie, issuer, storeID)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
                accessToken = response.getEntity().toString();
            }

            try (CloseableHttpResponse response = IAMClient.acquireStoreLock(cookie, storeID, accessToken)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
            }

            try (CloseableHttpResponse response = IAMClient.grantAccess(cookie, storeID, username, write, accessToken)) {
                IAMClient.releaseStoreLock(cookie, storeID, accessToken);
                IAMClient.deleteAccessToken(cookie, accessToken);
                return HttpUtils.buildResponse(response);
            }
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response revokeAccess(Cookie cookie, String storeID, String issuer, String username, boolean write) {
        try {
            String accessToken;
            try (CloseableHttpResponse response = IAMClient.createAccessToken(cookie, issuer, storeID)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
                accessToken = response.getEntity().toString();
            }

            try (CloseableHttpResponse response = IAMClient.acquireStoreLock(cookie, storeID, accessToken)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
            }

            try (CloseableHttpResponse response = IAMClient.revokeAccess(cookie, storeID, username, write, accessToken)) {
                IAMClient.releaseStoreLock(cookie, issuer, storeID);
                IAMClient.deleteAccessToken(cookie, accessToken);
                return HttpUtils.buildResponse(response);
            }
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

}
