package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.jena.graph.Triple;
import pt.fct.nova.id.srv.application.SPARQLQueryEngine;
import pt.fct.nova.id.srv.application.clients.HttpUtils;
import pt.fct.nova.id.srv.application.clients.IAMClient;
import pt.fct.nova.id.srv.application.clients.TriplestoreClient;
import pt.fct.nova.id.srv.application.query.plans.SimpleSPARQLPlanner;
import pt.fct.nova.id.srv.presentation.api.TriplestoreAPI;
import pt.fct.nova.id.srv.presentation.api.dtos.AccessForm;
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
    private static final String TRIPLESTORE_URI = System.getenv("TRIPLESTORE_URI");
    private static final String UPLOAD_TRIPLESTORE_PATH = TRIPLESTORE_URI.concat(System.getenv("UPLOAD_TRIPLESTORE_PATH"));
    private static final String QUERY_TRIPLESTORE_PATH = TRIPLESTORE_URI.concat(System.getenv("QUERY_TRIPLESTORE_PATH"));
    private static final String SUCCESSFUL_ACCESS_REQUEST = "Access request issued.";
    private static final String SUCCESSFUL_ACCESS_GRANT = "Access granted.";
    private static final String SUCCESSFUL_ACCESS_REVOCATION = "Access revoked.";

    private static final SPARQLQueryEngine queryEngine = new SPARQLQueryEngine(new SimpleSPARQLPlanner());


    @Override
    public Response create(Cookie cookie, UploadForm form) {
        try {
            String storeID = form.getStoreID();
            String issuer = form.getIssuer();
            try (CloseableHttpResponse response = IAMClient.createStore(cookie, storeID, issuer)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
            }

            String accessToken;
            try (CloseableHttpResponse response = IAMClient.getAccessToken(cookie, issuer, storeID)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
                accessToken = response.getEntity().toString();
            }
            return upload(cookie, storeID, parseTriples(form.getContents(), parseRDFLanguage(form.getSyntax())), accessToken);
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        } catch (UnknownRDFLanguageException e) {
            return Response.ok(INVALID_SYNTAX).status(Response.Status.BAD_REQUEST).build();
        }
    }

    @Override
    public Response listStores(Cookie cookie, String username, boolean write, boolean read, boolean owns) {
        try (CloseableHttpResponse response = IAMClient.listStores(cookie, username, write, read, owns)) {
            return HttpUtils.buildResponse(response);
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response upload(Cookie cookie, String storeID, UploadForm form) {
        try {
            String accessToken;
            try (CloseableHttpResponse response = IAMClient.getAccessToken(cookie, form.getIssuer(), storeID)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
                accessToken = response.getEntity().toString();
            }
            return upload(cookie, storeID, parseTriples(form.getContents(), parseRDFLanguage(form.getSyntax())), accessToken);
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        } catch (UnknownRDFLanguageException e) {
            return Response.ok(INVALID_SYNTAX).status(Response.Status.BAD_REQUEST).build();
        }
    }

    private Response upload(Cookie cookie, String storeID, List<Triple> triples, String accessToken) throws IOException {
        try (CloseableHttpResponse response = IAMClient.acquireStoreLock(cookie, storeID, accessToken)) {
            if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                return HttpUtils.buildResponse(response);
        }

        try (CloseableHttpResponse response = HttpUtils.sendPOSTRequest(cookie,
                String.format(TriplestoreController.UPLOAD_TRIPLESTORE_PATH, storeID),
                ClientUtils.objectToHttpEntity(triples),
                accessToken)) {
            IAMClient.releaseStoreLock(cookie, storeID, accessToken);
            return HttpUtils.buildResponse(response);
        }
    }

    @Override
    public Response delete(Cookie cookie, String storeID, String username) {
        try {
            String accessToken;
            try (CloseableHttpResponse response = IAMClient.getAccessToken(cookie, username, storeID)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
                accessToken = response.getEntity().toString();
            }

            try (CloseableHttpResponse response = IAMClient.acquireStoreLock(cookie, storeID, accessToken)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
            }

            try (CloseableHttpResponse response = TriplestoreClient.delete(cookie, storeID, accessToken)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode()) {
                    IAMClient.releaseStoreLock(cookie, username, storeID);
                    return HttpUtils.buildResponse(response);
                }
            }
            try (CloseableHttpResponse response = IAMClient.deleteStore(cookie, storeID, accessToken)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode()) {
                    IAMClient.releaseStoreLock(cookie, username, storeID);
                    return HttpUtils.buildResponse(response);
                }
            }
            return Response.ok(SUCCESSFUL_DELETION).build();
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response answerSPARQLQuery(Cookie cookie, String storeID, String query) {
        try (CloseableHttpResponse response = HttpUtils.sendPOSTRequest(cookie,
                String.format(QUERY_TRIPLESTORE_PATH, storeID),
                ClientUtils.objectToHttpEntity(queryEngine.getQueryPlan(query))
        )) {
            return HttpUtils.buildResponse(response);
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response requestAccess(Cookie cookie, String storeID, AccessForm form) {
        try {
            try (CloseableHttpResponse response = IAMClient.requestAccess(cookie, storeID, form)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
            }
            return Response.ok(SUCCESSFUL_ACCESS_REQUEST).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response grantAccess(Cookie cookie, String storeID, String username, boolean write) {
        try {
            String accessToken;
            try (CloseableHttpResponse response = IAMClient.getAccessToken(cookie, username, storeID)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
                accessToken = response.getEntity().toString();
            }

            try (CloseableHttpResponse response = IAMClient.acquireStoreLock(cookie, storeID, accessToken)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
            }

            try (CloseableHttpResponse response = IAMClient.grantAccess(cookie, storeID, write, accessToken)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode()) {
                    IAMClient.releaseStoreLock(cookie, username, storeID);
                    return HttpUtils.buildResponse(response);
                }
            }
            IAMClient.releaseStoreLock(cookie, username, storeID);
            return Response.ok(SUCCESSFUL_ACCESS_GRANT).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response revokeAccess(Cookie cookie, String storeID, String username, boolean write) {
        try {
            String accessToken;
            try (CloseableHttpResponse response = IAMClient.getAccessToken(cookie, username, storeID)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
                accessToken = response.getEntity().toString();
            }

            try (CloseableHttpResponse response = IAMClient.acquireStoreLock(cookie, storeID, accessToken)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
            }

            try (CloseableHttpResponse response = IAMClient.revokeAccess(cookie, storeID, write, accessToken)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode()) {
                    IAMClient.releaseStoreLock(cookie, username, storeID);
                    return HttpUtils.buildResponse(response);
                }
            }
            IAMClient.releaseStoreLock(cookie, username, storeID);
            return Response.ok(SUCCESSFUL_ACCESS_REVOCATION).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

}
