package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.core.Response;
import org.apache.http.client.HttpResponseException;
import pt.fct.nova.id.srv.presentation.api.TriplestoreAPI;
import pt.fct.nova.id.srv.presentation.api.dtos.UploadForm;

public class SecureTriplestoreController implements TriplestoreAPI {
    @Override
    public Response create(String storeID, UploadForm form) throws HttpResponseException {
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
