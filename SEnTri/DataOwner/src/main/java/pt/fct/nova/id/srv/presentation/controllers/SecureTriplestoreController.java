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
import pt.fct.nova.id.srv.application.clients.exception.TriplestoreDeleteException;
import pt.fct.nova.id.srv.application.protocols.Protocol2;
import pt.fct.nova.id.srv.application.protocols.ProtocolVersion;
import pt.fct.nova.id.srv.application.protocols.WriteProtocol;
import pt.fct.nova.id.srv.application.protocols.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.protocols.Protocol1;
import pt.fct.nova.id.srv.presentation.api.SecureTriplestoreAPI;
import pt.fct.nova.id.srv.presentation.api.dtos.SecureUploadForm;
import pt.fct.nova.id.srv.presentation.exceptions.UnknownRDFLanguageException;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

public class SecureTriplestoreController implements SecureTriplestoreAPI {
    private static final String INVALID_SYNTAX_MSG = "Invalid syntax: %s";
    private static final String PARSING_ERROR_MSG = "Error while parsing the file contents.";

    private static final String SUCCESS_UPLOAD = "Successful upload.";
    private static final String SUCCESS_CREATE = "Successful create.";
    private static final String BAD_NODE = "Data must only contain concrete nodes: IRI, Blank, Literal.";
    private static final String CREATE_ERROR = "Error during protocol execution while trying to create of triplestore %s: %s";
    private static final String ROLLBACK_ERROR = "Rollback Error: %s\nRoot: %s";



    @Override
    public Response create(ProtocolVersion protocolVersion, String storeID, SecureUploadForm form) {
        //TODO: check password && access
        try {
            List<Triple> triples = parseTriples(form.getContents(), parseRDFLanguage(form.getSyntax()));
            WriteProtocol protocol;
            switch (protocolVersion){
                case V1 -> {
                    protocol = new Protocol1(storeID);
                    Collections.shuffle(triples);
                    protocol.exec(triples);
                    StorageClient.saveProtocolSecrets(protocol);
                }
                case V2 -> {
                    protocol = new Protocol2();
                    Collections.shuffle(triples);
                    protocol.exec(triples);
                    StorageClient.saveProtocolSecrets(protocol);
                }
            }
            return Response.ok(SUCCESS_CREATE).build();
        } catch (UnknownRDFLanguageException e) {
            return Response.ok(String.format(INVALID_SYNTAX_MSG, form.getSyntax())).status(Response.Status.BAD_REQUEST).build();
        } catch (InvalidNodeException | TriplestoreClientException e) {
            return rollback(storeID, e);
        } catch (Exception e) {
            return Response.ok(PARSING_ERROR_MSG).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
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
    public Response upload(ProtocolVersion protocolVersion, String storeID, SecureUploadForm form) {
        return null;
    }

    @Override
    public Response answerSPARQLQuery(ProtocolVersion protocolVersion, String storeID, String query) {
        return null;
    }

}
