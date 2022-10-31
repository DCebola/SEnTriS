package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.core.Response;
import org.apache.http.client.HttpResponseException;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.system.AsyncParser;
import pt.fct.nova.id.srv.application.InvalidNodeException;
import pt.fct.nova.id.srv.application.triplestores.EncryptedTriplestore;
import pt.fct.nova.id.srv.presentation.api.TriplestoreAPI;
import pt.fct.nova.id.srv.presentation.api.dtos.UploadForm;

public class SecureTriplestoreController implements TriplestoreAPI {
    private static final String INVALID_SYNTAX_MSG = "Invalid syntax: %s";
    private static final String PARSING_ERROR_MSG = "Error while parsing the file contents.";
    private static final String SUCCESS_UPLOAD = "Successful upload.";
    private static final String BAD_NODE = "Data must only contain concrete nodes: IRI, Blank, Literal.";

    EncryptedTriplestore triplestore = new EncryptedTriplestore();


    @Override
    public Response create(String storeID, UploadForm form) throws HttpResponseException {
        Lang l = RDFLanguages.nameToLang(form.getSyntax());
        if (l == null)
            return Response.ok(String.format(INVALID_SYNTAX_MSG, form.getSyntax())).status(Response.Status.BAD_REQUEST).build();
        else {
            try {
                triplestore.createDataset(
                        storeID,
                        form.getPassword(),
                        AsyncParser.asyncParseTriples(form.getContents(), l, null)
                );
                return Response.ok(SUCCESS_UPLOAD).build();
            } catch (InvalidNodeException e) {
                return Response.ok(BAD_NODE).status(Response.Status.BAD_REQUEST).build();
            } catch (Exception e) {
                return Response.ok(PARSING_ERROR_MSG).status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        }
        return null;
    }

    @Override
    public Response upload(String storeID, UploadForm form) throws HttpResponseException {
        return null;
    }

    @Override
    public Response answerSPARQLQuery(String storeID, String query) {
        return null;
    }
}
