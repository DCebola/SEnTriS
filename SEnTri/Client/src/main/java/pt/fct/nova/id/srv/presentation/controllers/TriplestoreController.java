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
import pt.fct.nova.id.srv.application.clients.HTTPClient;
import pt.fct.nova.id.srv.application.clients.HTTPUtils;
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
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static jakarta.ws.rs.core.Response.Status.*;
import static pt.fct.nova.id.srv.presentation.controllers.ParsingUtils.*;
import static pt.fct.nova.id.srv.presentation.controllers.EncryptedTriplestoreController.SUCCESSFUL_DELETION;


@Path("triplestores")
public class TriplestoreController implements TriplestoreAPI {
    public static final String INVALID_SYNTAX = "Invalid syntax.";
    private static final String BAD_NODE = "Data must only contain concrete nodes: IRI, Blank, Literal.";
    private static final String NOT_IMPLEMENTED_ERROR = "Operation not yet supported.";
    private static final SPARQLQueryEngine queryEngine = new SPARQLQueryEngine(new SimpleSPARQLPlanner());

    @Override
    public Response create(Cookie cookie, UploadForm form) {
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            String triplestoreID = form.getTriplestoreID();
            String issuer = form.getIssuer();
            try (CloseableHttpResponse r = IAMClient.createTriplestore(httpClient, cookie, triplestoreID, issuer)) {
                if (r.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HTTPUtils.buildResponse(r);
            }

            String accessToken;
            try (CloseableHttpResponse response = IAMClient.createAccessToken(httpClient, cookie, issuer, triplestoreID)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HTTPUtils.buildResponse(response);
                accessToken = HTTPUtils.consumeResponseEntity(response);
            }
            return upload(httpClient, cookie, triplestoreID, parseTriples(form.getContents(), parseRDFLanguage(form.getSyntax())), accessToken);
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
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            try (CloseableHttpResponse response = IAMClient.listTriplestores(httpClient, cookie, issuer, write, read, owns)) {
                return HTTPUtils.buildResponse(response);
            } catch (Exception e) {
                return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
            }
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response upload(Cookie cookie, UploadForm form) {
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            String triplestoreID = form.getTriplestoreID();
            String accessToken;
            try (CloseableHttpResponse response = IAMClient.createAccessToken(httpClient, cookie, form.getIssuer(), triplestoreID)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HTTPUtils.buildResponse(response);
                accessToken = HTTPUtils.consumeResponseEntity(response);
            }
            return upload(httpClient, cookie, triplestoreID, parseTriples(form.getContents(), parseRDFLanguage(form.getSyntax())), accessToken);
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        } catch (UnknownRDFLanguageException e) {
            return Response.ok(INVALID_SYNTAX).status(Response.Status.BAD_REQUEST).build();
        } catch (InvalidNodeException e) {
            return Response.ok(BAD_NODE).status(Response.Status.BAD_REQUEST).build();
        }
    }

    private Response upload(HttpClient httpClient, Cookie cookie, String triplestoreID, List<Triple> triples, String accessToken) throws IOException, InvalidNodeException {
        try (CloseableHttpResponse response = IAMClient.acquireTriplestoreLock(httpClient, cookie, triplestoreID, accessToken)) {
            if (response.getStatusLine().getStatusCode() != OK.getStatusCode()) {
                Response errorResponse = HTTPUtils.buildResponse(response);
                try (CloseableHttpResponse ignore = IAMClient.deleteAccessToken(httpClient, cookie, triplestoreID, accessToken)) {
                    return errorResponse;
                }
            }
        }
        try (CloseableHttpResponse response = TriplestoreClient.upload(httpClient, cookie, triplestoreID, triples, accessToken);
             CloseableHttpResponse ignored = IAMClient.releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
             CloseableHttpResponse ignored2 = IAMClient.deleteAccessToken(httpClient, cookie, triplestoreID, accessToken)) {
            return HTTPUtils.buildResponse(response);
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

            try (CloseableHttpResponse response = TriplestoreClient.deleteAll(httpClient, cookie, triplestoreID, accessToken)) {
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
                return Response.ok(SUCCESSFUL_DELETION).build();
            }
        } catch (IOException e) {
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
            SimpleQueryExecutionPlan plan = (SimpleQueryExecutionPlan) queryEngine.getQueryPlan(form.getQuery());
            try (CloseableHttpResponse response = TriplestoreClient.query(httpClient, cookie, triplestoreID, plan, accessToken);
                 CloseableHttpResponse ignored = IAMClient.deleteAccessToken(httpClient, cookie, triplestoreID, accessToken)) {
                return HTTPUtils.buildResponse(response);
            }
        } catch (NotImplemented e) {
            return Response.ok(NOT_IMPLEMENTED_ERROR).status(INTERNAL_SERVER_ERROR).build();
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response updateTriplestoreOwner(Cookie cookie, String triplestoreID, String issuer, String target) {
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
            try (CloseableHttpResponse response = IAMClient.updateTriplestoreOwner(httpClient, cookie, triplestoreID, target, accessToken);
                 CloseableHttpResponse ignored = IAMClient.releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                 CloseableHttpResponse ignored2 = IAMClient.deleteAccessToken(httpClient, cookie, triplestoreID, accessToken)) {
                return HTTPUtils.buildResponse(response);
            }
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response issueAccessRequest(Cookie cookie, String triplestoreID, String issuer, boolean write) {
        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.requestAccess(httpClient, cookie, triplestoreID, issuer, write)) {
            return HTTPUtils.buildResponse(response);
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response listPendingAccessRequests(Cookie cookie, String triplestoreID, String issuer) {
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            String accessToken;
            try (CloseableHttpResponse response = IAMClient.createAccessToken(httpClient, cookie, issuer, triplestoreID)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HTTPUtils.buildResponse(response);
                accessToken = HTTPUtils.consumeResponseEntity(response);
            }

            try (CloseableHttpResponse response = IAMClient.listPendingAccessRequests(httpClient, cookie, triplestoreID, accessToken);
                 CloseableHttpResponse ignored = IAMClient.deleteAccessToken(httpClient, cookie, triplestoreID, accessToken)) {
                return HTTPUtils.buildResponse(response);
            }
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response processPendingAccessRequest(Cookie cookie, String triplestoreID, String issuer, String requestID, boolean accept) {
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            String accessToken;
            try (CloseableHttpResponse response = IAMClient.createAccessToken(httpClient, cookie, issuer, triplestoreID)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HTTPUtils.buildResponse(response);
                accessToken = HTTPUtils.consumeResponseEntity(response);
            }

            try (CloseableHttpResponse response = IAMClient.processAccessRequest(httpClient, cookie, triplestoreID, requestID, accept, accessToken);
                 CloseableHttpResponse ignored = IAMClient.deleteAccessToken(httpClient, cookie, triplestoreID, accessToken)) {
                return HTTPUtils.buildResponse(response);
            }
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }


    @Override
    public Response grantAccess(Cookie cookie, String triplestoreID, String issuer, String target, boolean write) {
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

            try (CloseableHttpResponse response = IAMClient.grantAccess(httpClient, cookie, triplestoreID, target, write, accessToken);
                 CloseableHttpResponse ignored = IAMClient.releaseTriplestoreLock(httpClient, cookie, issuer, triplestoreID);
                 CloseableHttpResponse ignored2 = IAMClient.deleteAccessToken(httpClient, cookie, triplestoreID, accessToken)) {
                return HTTPUtils.buildResponse(response);
            }
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response revokeAccess(Cookie cookie, String triplestoreID, String issuer, String target, boolean write) {
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

            try (CloseableHttpResponse response = IAMClient.revokeAccess(httpClient, cookie, triplestoreID, target, write, accessToken);
                 CloseableHttpResponse ignored = IAMClient.releaseTriplestoreLock(httpClient, cookie, issuer, triplestoreID);
                 CloseableHttpResponse ignored2 = IAMClient.deleteAccessToken(httpClient, cookie, triplestoreID, accessToken)) {
                return HTTPUtils.buildResponse(response);
            }

        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response listUsersWithAccess(Cookie cookie, String triplestoreID, String issuer, boolean write) {
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            String accessToken;
            try (CloseableHttpResponse response = IAMClient.createAccessToken(httpClient, cookie, issuer, triplestoreID)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HTTPUtils.buildResponse(response);
                accessToken = HTTPUtils.consumeResponseEntity(response);
            }

            try (CloseableHttpResponse response = IAMClient.listUsersWithAccess(httpClient, cookie, triplestoreID, write, accessToken);
                 CloseableHttpResponse ignored = IAMClient.deleteAccessToken(httpClient, cookie, triplestoreID, accessToken)) {
                return HTTPUtils.buildResponse(response);
            }
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

}
