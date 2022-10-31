package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.core.Response;
import org.apache.http.client.HttpResponseException;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.system.AsyncParser;
import pt.fct.nova.id.srv.application.protocols.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.protocols.Protocol1;
import pt.fct.nova.id.srv.presentation.api.SecureTriplestoreAPI;
import pt.fct.nova.id.srv.presentation.api.dtos.SecureUploadForm;
import pt.fct.nova.id.srv.presentation.api.dtos.UploadForm;
import pt.fct.nova.id.srv.presentation.exceptions.UnknownRDFLanguageException;

public class SecureTriplestoreController implements SecureTriplestoreAPI {
    private static final String INVALID_SYNTAX_MSG = "Invalid syntax: %s";
    private static final String PARSING_ERROR_MSG = "Error while parsing the file contents.";
    private static final String SUCCESS_UPLOAD = "Successful upload.";
    private static final String BAD_NODE = "Data must only contain concrete nodes: IRI, Blank, Literal.";


    @Override
    public Response create(String storeID, SecureUploadForm form) {
        //TODO: verify password to access/save keys/secrets.
        try {
            new Protocol1(storeID).exec(
                    AsyncParser.asyncParseTriples(form.getContents(), parseRDFLanguage(form.getSyntax()), null)
            );
            return Response.ok(SUCCESS_UPLOAD).build();
        } catch (InvalidNodeException e) {
            return Response.ok(BAD_NODE).status(Response.Status.BAD_REQUEST).build();
        } catch (UnknownRDFLanguageException e) {
            return Response.ok(String.format(INVALID_SYNTAX_MSG, form.getSyntax())).status(Response.Status.BAD_REQUEST).build();
        } catch (Exception e) {
            return Response.ok(PARSING_ERROR_MSG).status(Response.Status.INTERNAL_SERVER_ERROR).build();
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
        return null;
    }

}
