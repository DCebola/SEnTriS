package pt.fct.nova.id.srv.presentation.controllers;

import com.google.gson.Gson;
import jakarta.ws.rs.core.Response;
import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.lang.CollectorStreamTriples;
import pt.fct.nova.id.srv.application.clients.LockClient;
import pt.fct.nova.id.srv.application.clients.SecretsClient;
import pt.fct.nova.id.srv.application.clients.TriplestoreClient;
import pt.fct.nova.id.srv.application.clients.exception.*;
import pt.fct.nova.id.srv.application.protocols.ProtocolVersion;
import pt.fct.nova.id.srv.application.protocols.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.protocols.Protocol1;
import pt.fct.nova.id.srv.presentation.api.SecureTriplestoreAPI;
import pt.fct.nova.id.srv.presentation.api.dtos.SecureUploadForm;
import pt.fct.nova.id.srv.presentation.exceptions.UnknownRDFLanguageException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.io.InputStream;
import java.security.*;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static pt.fct.nova.id.srv.application.clients.SecretsClient.*;

public class SecureTriplestoreController implements SecureTriplestoreAPI {
    private static final String INVALID_SYNTAX_MSG = "Invalid syntax: %s";
    private static final String PARSING_ERROR_MSG = "Error while parsing the file contents.";
    private static final String STORE_ALREADY_EXISTS = "Store %s already exists.";
    private static final String STORE_NOT_FOUND = "Store %s not found.";
    private static final String SUCCESS_UPLOAD = "Successful upload.";
    private static final String SUCCESS_CREATE = "Successful create.";
    private static final String BAD_NODE = "Data must only contain concrete nodes: IRI, Blank, Literal.";
    private static final String CREATE_ERROR = "Error during protocol execution while trying to create triplestore %s: %s";
    private static final String UPLOAD_ERROR = "Error during protocol execution while trying to upload to triplestore %s: %s";
    private static final String ROLLBACK_ERROR = "Rollback Error: %s";
    private static final String NOT_IMPLEMENTED = "Not implemented.";
    private static final String OPERATION_TIMEOUT = "Operation timeout.";
    private static final Gson gson = new Gson();


    @Override
    public Response create(ProtocolVersion protocolVersion, String storeID, SecureUploadForm form) {
        //TODO: check session and access
        if (SecretsClient.exists(storeID))
            return Response.ok(String.format(STORE_ALREADY_EXISTS, storeID)).status(Response.Status.BAD_REQUEST).build();
        try {
            List<Triple> triples = parseTriples(form.getContents(), parseRDFLanguage(form.getSyntax()));
            switch (protocolVersion) {
                case V1 -> {
                    Protocol1 p = new Protocol1(storeID);
                    Collections.shuffle(triples);
                    p.exec(triples);
                    try {
                        String lockID = LockClient.acquireLock(storeID);
                        if (lockID == null)
                            return Response.ok(OPERATION_TIMEOUT).status(Response.Status.INTERNAL_SERVER_ERROR).build();
                        TriplestoreClient.create(storeID, p.getEncryptedT());
                        SecretsClient.saveProtocolSecrets(p);
                        LockClient.releaseLock(storeID, lockID);
                    } catch (TriplestoreClientException e) {
                        return Response.ok(String.format(CREATE_ERROR, storeID, e.getMessage())).status(Response.Status.BAD_REQUEST).build();
                    }
                }
                case V2 -> {
                    //TODO: Create protocol v2
                }
            }
            return Response.ok(SUCCESS_CREATE).build();
        } catch (UnknownRDFLanguageException e) {
            return Response.ok(String.format(INVALID_SYNTAX_MSG, form.getSyntax())).status(Response.Status.BAD_REQUEST).build();
        } catch (InvalidNodeException e) {
            return Response.ok(BAD_NODE).status(Response.Status.BAD_REQUEST).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok(PARSING_ERROR_MSG).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

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

    @Override
    public Response upload(String storeID, SecureUploadForm form) {
        //TODO: check password && access
        try {
            List<String> secrets = SecretsClient.getProtocolSecrets(storeID);
            if (secrets.isEmpty())
                return Response.ok(String.format(STORE_NOT_FOUND, storeID)).status(Response.Status.NOT_FOUND).build();
            List<Triple> triples = parseTriples(form.getContents(), parseRDFLanguage(form.getSyntax()));
            switch (ProtocolVersion.fromString(secrets.get(PROTOCOL_VERSION))) {
                case V1 -> {
                    Protocol1 p = initProtocol1(storeID, secrets);
                    Collections.shuffle(triples);
                    Map<String, Pair<Integer, byte[]>> keywords = p.fetchKeywords(triples);
                    p.updateKeywords(keywords);
                    Collections.shuffle(triples);
                    p.exec(triples);
                    try {
                        String lockID = LockClient.acquireLock(storeID);
                        if (lockID == null)
                            return Response.ok(OPERATION_TIMEOUT).status(Response.Status.INTERNAL_SERVER_ERROR).build();
                        TriplestoreClient.upload(storeID, p.getEncryptedT());
                        SecretsClient.saveProtocolSecrets(p);
                        LockClient.releaseLock(storeID, lockID);
                    } catch (TriplestoreClientException e) {
                        return Response.ok(String.format(UPLOAD_ERROR, storeID, e.getMessage())).status(Response.Status.BAD_REQUEST).build();
                    }
                }
                case V2 -> {
                    //TODO: Upload protocol v2
                }
            }
            return Response.ok(SUCCESS_UPLOAD).build();
        } catch (UnknownRDFLanguageException e) {
            return Response.ok(String.format(INVALID_SYNTAX_MSG, form.getSyntax())).status(Response.Status.BAD_REQUEST).build();
        } catch (InvalidNodeException e) {
            return Response.ok(BAD_NODE).status(Response.Status.BAD_REQUEST).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok(PARSING_ERROR_MSG).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private static Protocol1 initProtocol1(String storeID, List<String> secrets) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        SecretKey k1 = gson.fromJson(secrets.get(P1_KEY_1), SecretKey.class);
        SecretKey k2 = gson.fromJson(secrets.get(P1_KEY_2), SecretKey.class);
        SecretKey k3 = gson.fromJson(secrets.get(P1_KEY_3), SecretKey.class);
        byte[] iv = decodeBase64(secrets.get(P1_IV));
        return new Protocol1(storeID, k1, k2, k3, iv);
    }


    @Override
    public Response answerSPARQLQuery(ProtocolVersion protocolVersion, String storeID, String query) {
        return Response.ok(NOT_IMPLEMENTED).status(Response.Status.NOT_IMPLEMENTED).build();
    }

}
