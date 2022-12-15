package pt.fct.nova.id.srv.presentation.controllers;


import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import pt.fct.nova.id.srv.presentation.api.ProxyAPI;

import java.util.List;

@Path("/queries")
public class QueriesController implements ProxyAPI {
    @Override
    public Response answerSPARQLQuery(byte[] queryExecutionPlan, List<String> authorizationHeaders) {
        return null;
    }

    @Override
    public Response saveBinding(List<String> encryptedNodes, List<String> authorizationHeaders) {
        return null;
    }
}
