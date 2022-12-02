package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.jena.graph.Triple;
import pt.fct.nova.id.srv.application.clients.*;
import pt.fct.nova.id.srv.application.protocols.ProtocolVersion;
import pt.fct.nova.id.srv.application.protocols.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.protocols.Protocol1;
import pt.fct.nova.id.srv.presentation.api.SecureTriplestoreAPI;
import pt.fct.nova.id.srv.presentation.api.dtos.SecureCreateForm;
import pt.fct.nova.id.srv.presentation.api.dtos.SecureQueryForm;
import pt.fct.nova.id.srv.presentation.api.dtos.SecureUploadForm;
import pt.fct.nova.id.srv.presentation.exceptions.MalformedSecretsException;
import pt.fct.nova.id.srv.presentation.exceptions.UnknownRDFLanguageException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static jakarta.ws.rs.core.Response.Status.*;
import static pt.fct.nova.id.srv.presentation.controllers.ClientUtils.*;
import static pt.fct.nova.id.srv.presentation.controllers.TriplestoreController.INVALID_SYNTAX;

@Path("triplestore/secure")
public class SecureTriplestoreController implements SecureTriplestoreAPI {
    public static final String SECRETS_VERSION = System.getenv("SECRETS_PROTOCOL_VERSION");
    public static final String SECRETS_KEY = System.getenv("SECRETS_PROTOCOL_KEY");
    public static final String SECRETS_IV = System.getenv("SECRETS_PROTOCOL_IV");
    private static final String INTERNAL_ERROR = "Internal error.";
    private static final String SUCCESSFUL_UPLOAD = "Successful upload.";
    private static final String SUCCESSFUL_CREATE = "Successful create.";
    public static final String SUCCESSFUL_DELETION = "Successful deletion.";
    private static final String BAD_NODE = "Data must only contain concrete nodes: IRI, Blank, Literal.";
    private static final String NOT_IMPLEMENTED = "Not implemented.";
    private static final String MALFORMED_SECRETS = "Secrets malformed.";


    @Override
    public Response create(Cookie cookie, SecureCreateForm form) {
        try {
            String triplestoreID = form.getTriplestoreID();
            String issuer = form.getIssuer();
            try (CloseableHttpResponse response = IAMClient.createTriplestore(cookie, triplestoreID, issuer)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
            }

            String accessToken;
            try (CloseableHttpResponse response = IAMClient.createAccessToken(cookie, issuer, triplestoreID)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
                accessToken = response.getEntity().toString();
            }
            try (CloseableHttpResponse response = IAMClient.acquireTriplestoreLock(cookie, triplestoreID, accessToken)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
            }
            List<Triple> triples = parseTriples(form.getContents(), parseRDFLanguage(form.getSyntax()));
            switch (form.getProtocolVersion()) {
                case V1 -> {
                    Protocol1 p = new Protocol1(triplestoreID);
                    try (CloseableHttpResponse response = VaultClient.saveProtocolSecrets(cookie, triplestoreID, generateSecretsMap(p), accessToken)) {
                        if (response.getStatusLine().getStatusCode() != OK.getStatusCode()) {
                            try (CloseableHttpResponse response2 = IAMClient.deleteTriplestore(cookie, triplestoreID, accessToken)) {
                                if (response2.getStatusLine().getStatusCode() != OK.getStatusCode()) {
                                    IAMClient.releaseTriplestoreLock(cookie, triplestoreID, accessToken);
                                    IAMClient.deleteAccessToken(cookie, accessToken);
                                    return HttpUtils.buildResponse("Failure to delete triplestore. Error occurred while saving secrets: ", response2);
                                }
                            }
                            IAMClient.releaseTriplestoreLock(cookie, triplestoreID, accessToken);
                            IAMClient.deleteAccessToken(cookie, accessToken);
                            return HttpUtils.buildResponse(response);
                        }
                    }

                    Collections.shuffle(triples);
                    p.exec(triples);

                    try (CloseableHttpResponse response = SecureTriplestoreClient.upload(cookie, triplestoreID, p.getEncryptedT(), accessToken)) {
                        IAMClient.releaseTriplestoreLock(cookie, triplestoreID, accessToken);
                        IAMClient.deleteAccessToken(cookie, accessToken);
                        return HttpUtils.buildResponse(response);
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
    public Response delete(Cookie cookie, String triplestoreID, String username) {
        try {
            String accessToken;
            try (CloseableHttpResponse response = IAMClient.createAccessToken(cookie, username, triplestoreID)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
                accessToken = response.getEntity().toString();
            }

            try (CloseableHttpResponse response = IAMClient.acquireTriplestoreLock(cookie, triplestoreID, accessToken)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
            }

            try (CloseableHttpResponse response = SecureTriplestoreClient.deleteAll(cookie, triplestoreID, accessToken)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode()) {
                    IAMClient.releaseTriplestoreLock(cookie, triplestoreID, accessToken);
                    IAMClient.deleteAccessToken(cookie, accessToken);
                    return HttpUtils.buildResponse(response);
                }
            }
            try (CloseableHttpResponse response = VaultClient.deleteProtocolSecrets(cookie, triplestoreID, accessToken)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode()) {
                    IAMClient.releaseTriplestoreLock(cookie, triplestoreID, accessToken);
                    IAMClient.deleteAccessToken(cookie, accessToken);
                    return HttpUtils.buildResponse(response);
                }
            }
            try (CloseableHttpResponse response = IAMClient.deleteTriplestore(cookie, triplestoreID, accessToken)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode()) {
                    IAMClient.releaseTriplestoreLock(cookie, triplestoreID, accessToken);
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
    public Response upload(Cookie cookie, String triplestoreID, SecureUploadForm form) {
        try {
            String issuer = form.getIssuer();
            Map<String, String> secrets = ClientUtils.sanitizeSecrets(form.getSecrets());

            String accessToken;
            try (CloseableHttpResponse response = IAMClient.createAccessToken(cookie, issuer, triplestoreID)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
                accessToken = response.getEntity().toString();
            }

            if (secrets.isEmpty()) {
                try (CloseableHttpResponse response = VaultClient.getProtocolSecrets(cookie, triplestoreID, accessToken)) {
                    if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                        return HttpUtils.buildResponse(response);
                    secrets = ClientUtils.parseSecrets(response.getEntity().toString());
                }
            }
            try (CloseableHttpResponse response = IAMClient.acquireTriplestoreLock(cookie, triplestoreID, accessToken)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
            }
            List<Triple> triples = parseTriples(form.getContents(), parseRDFLanguage(form.getSyntax()));
            switch (ProtocolVersion.fromString(secrets.get(SECRETS_VERSION))) {
                case V1 -> {
                    Protocol1 p = initProtocol1(triplestoreID, secrets);
                    Collections.shuffle(triples);
                    try (Response response = fetchAndUpdateKeywords(cookie, triplestoreID, p.generateKeywordTrapdoorMap(triples), p, accessToken)) {
                        if (response != null) {
                            IAMClient.releaseTriplestoreLock(cookie, triplestoreID, accessToken);
                            IAMClient.deleteAccessToken(cookie, accessToken);
                            return response;
                        }
                    }
                    Collections.shuffle(triples);
                    p.exec(triples);
                    try (CloseableHttpResponse response = SecureTriplestoreClient.upload(cookie, triplestoreID, p.getEncryptedT(), accessToken)) {
                        IAMClient.releaseTriplestoreLock(cookie, triplestoreID, accessToken);
                        IAMClient.deleteAccessToken(cookie, accessToken);
                        return HttpUtils.buildResponse(response);

                    }
                }
                case V2 -> {
                    //TODO: Create protocol v2
                    return Response.ok(NOT_IMPLEMENTED).status(Response.Status.NOT_IMPLEMENTED).build();
                }

                default ->
                        throw new IllegalStateException("Unexpected value: " + ProtocolVersion.fromString(secrets.get(SECRETS_VERSION)));
            }
        } catch (MalformedSecretsException e) {
            return Response.ok(MALFORMED_SECRETS, triplestoreID).status(Response.Status.BAD_REQUEST).build();
        } catch (UnknownRDFLanguageException e) {
            return Response.ok(INVALID_SYNTAX).status(Response.Status.BAD_REQUEST).build();
        } catch (InvalidNodeException e) {
            return Response.ok(BAD_NODE).status(Response.Status.BAD_REQUEST).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response answerSPARQLQuery(Cookie cookie, SecureQueryForm form) {
        try {
            String accessToken;
            String triplestoreID = form.getTriplestoreID();
            try (CloseableHttpResponse response = IAMClient.createAccessToken(cookie, form.getIssuer(), triplestoreID)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
                accessToken = response.getEntity().toString();
            }
            /*
            try (CloseableHttpResponse response = SecureTriplestoreClient.query(cookie, triplestoreID, queryEngine.getQueryPlan(form.getQuery()), accessToken)) {
                return HttpUtils.buildResponse(response);
            }
            */
            return Response.ok(NOT_IMPLEMENTED).status(Response.Status.NOT_IMPLEMENTED).build();
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }


    private Response fetchAndUpdateKeywords(Cookie cookie, String triplestoreID, Map<String, String> keywordTrapdoorMap, Protocol1 protocol, String accessToken) throws InvalidNodeException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, IOException, InvalidKeyException {
        List<String> trapdoors = new ArrayList<>(keywordTrapdoorMap.size());
        List<String> keywords = new ArrayList<>(keywordTrapdoorMap.size());
        int i = 0;
        for (Map.Entry<String, String> entry : keywordTrapdoorMap.entrySet()) {
            trapdoors.add(entry.getKey());
            keywords.add(i, entry.getValue());
            i++;
        }

        List<String> keywordsTotals;
        try (CloseableHttpResponse response = SecureTriplestoreClient.search(cookie, triplestoreID, trapdoors, accessToken)) {
            if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                return HttpUtils.buildResponse(response);
            keywordsTotals = ClientUtils.parseSearchResults(EntityUtils.toString(response.getEntity()));
        }
        protocol.updateKeywords(protocol.generateKeywordIVMap(keywords, keywordsTotals));
        return null;
    }

}
