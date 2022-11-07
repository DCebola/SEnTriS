package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.core.Response;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.lang.CollectorStreamTriples;
import pt.fct.nova.id.srv.application.clients.StorageClient;
import pt.fct.nova.id.srv.application.clients.TriplestoreClient;
import pt.fct.nova.id.srv.application.clients.exception.TriplestoreClientException;
import pt.fct.nova.id.srv.application.clients.exception.TriplestoreCreateException;
import pt.fct.nova.id.srv.application.clients.exception.TriplestoreDeleteException;
import pt.fct.nova.id.srv.application.clients.exception.TriplestoreUploadException;
import pt.fct.nova.id.srv.application.crypto.KeyStoreUtils;
import pt.fct.nova.id.srv.application.protocols.Protocol2;
import pt.fct.nova.id.srv.application.protocols.ProtocolVersion;
import pt.fct.nova.id.srv.application.protocols.EncryptionProtocol;
import pt.fct.nova.id.srv.application.protocols.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.protocols.Protocol1;
import pt.fct.nova.id.srv.application.protocols.exceptions.UnknownProtocolException;
import pt.fct.nova.id.srv.presentation.api.SecureTriplestoreAPI;
import pt.fct.nova.id.srv.presentation.api.dtos.SecureUploadForm;
import pt.fct.nova.id.srv.presentation.exceptions.UnknownRDFLanguageException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static pt.fct.nova.id.srv.application.clients.StorageClient.*;

public class SecureTriplestoreController implements SecureTriplestoreAPI {
    private static final String INVALID_SYNTAX_MSG = "Invalid syntax: %s";
    private static final String PARSING_ERROR_MSG = "Error while parsing the file contents.";
    private static final String STORE_ALREADY_EXISTS = "Store %s already exists.";
    private static final String STORE_NOT_FOUND = "Store %s not found.";
    private static final String SUCCESS_UPLOAD = "Successful upload.";
    private static final String SUCCESS_CREATE = "Successful create.";
    private static final String BAD_NODE = "Data must only contain concrete nodes: IRI, Blank, Literal.";
    private static final String CREATE_ERROR = "Error during protocol execution while trying to create of triplestore %s: %s";
    private static final String ROLLBACK_ERROR = "Rollback Error: %s\nRoot: %s";

    @Override
    public Response create(ProtocolVersion protocolVersion, String storeID, SecureUploadForm form) {
        //TODO: check password && access
        if (StorageClient.exists(storeID))
            return Response.ok(String.format(STORE_ALREADY_EXISTS, storeID)).status(Response.Status.BAD_REQUEST).build();
        try {
            List<Triple> triples = parseTriples(form.getContents(), parseRDFLanguage(form.getSyntax()));
            switch (protocolVersion) {
                case V1 -> {
                    if (form.isBatched())
                        execEncryptionProtocol(new Protocol1(form.getBatchLength(), storeID), triples, form.getPassword());
                    else
                        execEncryptionProtocol(new Protocol1(storeID), triples, form.getPassword());
                }
                case V2 -> {
                    if (form.isBatched())
                        execEncryptionProtocol(new Protocol2(), triples, form.getPassword());
                    else
                        execEncryptionProtocol(new Protocol2(), triples, form.getPassword());
                }
            }
            return Response.ok(SUCCESS_CREATE).build();
        } catch (UnknownRDFLanguageException e) {
            return Response.ok(String.format(INVALID_SYNTAX_MSG, form.getSyntax())).status(Response.Status.BAD_REQUEST).build();
        } catch (InvalidNodeException | TriplestoreClientException e) {
            return rollback(storeID, e);
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok(PARSING_ERROR_MSG).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private static void execEncryptionProtocol(EncryptionProtocol protocol, List<Triple> triples, char[] password) throws NoSuchAlgorithmException, InvalidNodeException, UnknownProtocolException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, TriplestoreCreateException, UnsupportedEncodingException, TriplestoreUploadException, KeyStoreException {
        Collections.shuffle(triples);
        protocol.exec(triples);
        StorageClient.saveProtocolSecrets(protocol, password);
    }

    private List<Triple> parseTriples(InputStream content, Lang lang) throws UnknownRDFLanguageException {
        CollectorStreamTriples tripleCollector = new CollectorStreamTriples();
        RDFParser.source(content).lang(lang).parse(tripleCollector);
        return tripleCollector.getCollected();
    }

    private Response rollback(String storeID, Exception e) {
        try {
            TriplestoreClient.delete(storeID);
            if (e instanceof InvalidNodeException)
                return Response.ok(BAD_NODE).status(Response.Status.BAD_REQUEST).build();
            else
                return Response.ok(String.format(CREATE_ERROR, storeID, e.getMessage())).status(Response.Status.BAD_REQUEST).build();
        } catch (TriplestoreDeleteException e2) {
            return Response.ok(String.format(ROLLBACK_ERROR, e2.getMessage(), BAD_NODE))
                    .status(Response.Status.BAD_REQUEST).build();
        }
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
            List<String> secrets = StorageClient.getProtocolSecrets(storeID);
            if (secrets.isEmpty())
                return Response.ok(String.format(STORE_NOT_FOUND, storeID)).status(Response.Status.NOT_FOUND).build();
            List<Triple> triples = parseTriples(form.getContents(), parseRDFLanguage(form.getSyntax()));
            switch (ProtocolVersion.fromString(secrets.get(PROTOCOL_VERSION))) {
                case V1 -> {
                    Protocol1 p = initProtocol1(storeID, form, secrets);
                    Collections.shuffle(triples);
                    //TODO: batch update
                    p.updateKeywords(p.fetchKeywords(triples));
                    execEncryptionProtocol(p, triples, form.getPassword());
                }
                case V2 -> {
                    if (form.isBatched())
                        execEncryptionProtocol(new Protocol2(), triples, form.getPassword());
                    else
                        execEncryptionProtocol(new Protocol2(), triples, form.getPassword());
                }
            }
            return Response.ok(SUCCESS_UPLOAD).build();
        } catch (UnknownRDFLanguageException e) {
            return Response.ok(String.format(INVALID_SYNTAX_MSG, form.getSyntax())).status(Response.Status.BAD_REQUEST).build();
        } catch (InvalidNodeException | TriplestoreClientException e) {
            return rollback(storeID, e);
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok(PARSING_ERROR_MSG).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private static Protocol1 initProtocol1(String storeID, SecureUploadForm form, List<String> secrets) throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException {
        SecretKey k1 = KeyStoreUtils.getSecretKey(secrets.get(P1_KEY_1), form.getPassword());
        SecretKey k2 = KeyStoreUtils.getSecretKey(secrets.get(P1_KEY_2), form.getPassword());
        SecretKey k3 = KeyStoreUtils.getSecretKey(secrets.get(P1_KEY_3), form.getPassword());
        byte[] iv = decodeBase64(secrets.get(P1_IV));
        if (form.isBatched())
            return new Protocol1(form.getBatchLength(), storeID, k1, k2, k3, iv);
        else
            return new Protocol1(storeID, k1, k2, k3, iv);
    }

    @Override
    public Response answerSPARQLQuery(ProtocolVersion protocolVersion, String storeID, String query) {
        return null;
    }

}
