package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.lang.CollectorStreamTriples;
import pt.fct.nova.id.srv.application.clients.HttpUtils;
import pt.fct.nova.id.srv.application.clients.IAMClient;
import pt.fct.nova.id.srv.application.clients.VaultClient;
import pt.fct.nova.id.srv.application.clients.TriplestoreClient;
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
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static jakarta.ws.rs.core.Response.Status.OK;

@Path("triplestore/secure")
public class SecureTriplestoreController implements SecureTriplestoreAPI {
    public static final String SECRETS_VERSION_KEY = System.getenv("PROTOCOL_VERSION_KEY");
    public static final String SECRETS_NTH_KEY = System.getenv("PROTOCOL_KEY").concat("_%s");
    public static final String SECRETS_IV = System.getenv("PROTOCOL_KEY");
    private static final String INVALID_SYNTAX = "Invalid syntax.";
    private static final String INTERNAL_ERROR = "Internal error.";
    private static final String SUCCESS_UPLOAD = "Successful upload.";
    private static final String SUCCESS_CREATE = "Successful create.";
    private static final String BAD_NODE = "Data must only contain concrete nodes: IRI, Blank, Literal.";
    private static final String NOT_IMPLEMENTED = "Not implemented.";
    private static final String MALFORMED_SECRETS = "Secrets malformed.";

    @Override
    public Response create(Cookie cookie, SecureCreateForm form) {
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
            try (CloseableHttpResponse response = IAMClient.acquireStoreLock(cookie, storeID, accessToken)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
            }
            List<Triple> triples = parseTriples(form.getContents(), parseRDFLanguage(form.getSyntax()));
            switch (form.getProtocolVersion()) {
                case V1 -> {
                    Protocol1 p = new Protocol1(storeID);
                    try (CloseableHttpResponse response = VaultClient.saveProtocolSecrets(cookie, storeID, ClientUtils.generateSecretsMap(p), accessToken)) {
                        if (response.getStatusLine().getStatusCode() != OK.getStatusCode()) {
                            IAMClient.deleteStore(cookie, storeID, accessToken);
                            return HttpUtils.buildResponse(response);
                        }
                    }

                    Collections.shuffle(triples);
                    p.exec(triples);

                    try (CloseableHttpResponse response = TriplestoreClient.upload(cookie, storeID, p.getEncryptedT(), accessToken)) {
                        if (response.getStatusLine().getStatusCode() != OK.getStatusCode()) {
                            IAMClient.releaseStoreLock(cookie, storeID, accessToken);
                            return HttpUtils.buildResponse(response);
                        }
                    }
                    IAMClient.releaseStoreLock(cookie, storeID, accessToken);
                }
                case V2 -> {
                    //TODO: Create protocol v2
                }
            }
            return Response.ok(SUCCESS_CREATE).build();
        } catch (UnknownRDFLanguageException e) {
            return Response.ok(INVALID_SYNTAX).status(Response.Status.BAD_REQUEST).build();
        } catch (InvalidNodeException e) {
            return Response.ok(BAD_NODE).status(Response.Status.BAD_REQUEST).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response upload(Cookie cookie, String storeID, SecureUploadForm form) {
        try {
            String issuer = form.getIssuer();
            Map<String, String> secrets = ClientUtils.sanitizeSecrets(form.getSecrets());

            String accessToken;
            try (CloseableHttpResponse response = IAMClient.getAccessToken(cookie, issuer, storeID)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
                accessToken = response.getEntity().toString();
            }

            if (secrets.isEmpty()) {
                try (CloseableHttpResponse response = VaultClient.getProtocolSecrets(cookie, storeID, accessToken)) {
                    if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                        return HttpUtils.buildResponse(response);
                    secrets = ClientUtils.parseSecrets(response.getEntity().toString());
                }
            }
            try (CloseableHttpResponse response = IAMClient.acquireStoreLock(cookie, storeID, accessToken)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
            }
            List<Triple> triples = parseTriples(form.getContents(), parseRDFLanguage(form.getSyntax()));
            switch (ProtocolVersion.fromString(secrets.get(SECRETS_VERSION_KEY))) {
                case V1 -> {
                    Protocol1 p = ClientUtils.initProtocol1(storeID, secrets);
                    Collections.shuffle(triples);
                    try (Response response = fetchAndUpdateKeywords(cookie, storeID, p.generateKeywordTrapdoorMap(triples), p, accessToken)) {
                        if (response != null) {
                            IAMClient.releaseStoreLock(cookie, storeID, accessToken);
                            return response;
                        }
                    }
                    Collections.shuffle(triples);
                    p.exec(triples);
                    try (CloseableHttpResponse response = TriplestoreClient.upload(cookie, storeID, p.getEncryptedT(), accessToken)) {
                        if (response.getStatusLine().getStatusCode() != OK.getStatusCode()) {
                            IAMClient.releaseStoreLock(cookie, storeID, accessToken);
                            return HttpUtils.buildResponse(response);
                        }
                    }
                    IAMClient.releaseStoreLock(cookie, storeID, accessToken);
                }
                case V2 -> {
                    //TODO: Upload protocol v2
                }
            }
            return Response.ok(SUCCESS_UPLOAD).build();
        } catch (MalformedSecretsException e) {
            return Response.ok(MALFORMED_SECRETS, storeID).status(Response.Status.BAD_REQUEST).build();
        } catch (UnknownRDFLanguageException e) {
            return Response.ok(String.format(INVALID_SYNTAX, form.getSyntax())).status(Response.Status.BAD_REQUEST).build();
        } catch (InvalidNodeException e) {
            return Response.ok(BAD_NODE).status(Response.Status.BAD_REQUEST).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response answerSPARQLQuery(Cookie cookie, String storeID, SecureQueryForm form) {
        return Response.ok(NOT_IMPLEMENTED).status(Response.Status.NOT_IMPLEMENTED).build();
    }

    private List<Triple> parseTriples(InputStream content, Lang lang) throws UnknownRDFLanguageException {
        CollectorStreamTriples tripleCollector = new CollectorStreamTriples();
        RDFParser.source(content).lang(lang).parse(tripleCollector);
        return tripleCollector.getCollected();
    }

    private Lang parseRDFLanguage(String syntax) throws UnknownRDFLanguageException {
        Lang l = RDFLanguages.nameToLang(syntax);
        if (l == null)
            throw new UnknownRDFLanguageException();
        return l;
    }

    private Response fetchAndUpdateKeywords(Cookie cookie, String storeID, Map<String, String> keywordTrapdoorMap, Protocol1 protocol, String accessToken) throws InvalidNodeException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, IOException, InvalidKeyException {
        List<String> trapdoors = new ArrayList<>(keywordTrapdoorMap.size());
        List<String> keywords = new ArrayList<>(keywordTrapdoorMap.size());
        int i = 0;
        for (Map.Entry<String, String> entry : keywordTrapdoorMap.entrySet()) {
            trapdoors.add(entry.getKey());
            keywords.add(i, entry.getValue());
            i++;
        }

        List<String> keywordsTotals;
        try (CloseableHttpResponse response = TriplestoreClient.search(cookie, storeID, trapdoors, accessToken)) {
            if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                return HttpUtils.buildResponse(response);
            keywordsTotals = ClientUtils.parseSearchResults(EntityUtils.toString(response.getEntity()));
        }
        protocol.updateKeywords(protocol.generateKeywordIVMap(keywords, keywordsTotals));
        return null;
    }

}
