package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import pt.fct.nova.id.srv.application.clients.HTTPClient;
import pt.fct.nova.id.srv.application.clients.HTTPUtils;
import pt.fct.nova.id.srv.application.clients.IAMClient;
import pt.fct.nova.id.srv.application.clients.ProxyClient;
import pt.fct.nova.id.srv.application.storage.EncryptedStorageEngine;
import pt.fct.nova.id.srv.application.storage.redis.EncryptedTriplestoreStorageEngine;
import pt.fct.nova.id.srv.presentation.apis.EncryptedTriplestoreV1API;
import pt.fct.nova.id.srv.presentation.dtos.UpdateForm;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.*;

import static jakarta.ws.rs.core.Response.Status.*;
import static pt.fct.nova.id.srv.application.clients.HTTPUtils.extractAccessToken;

@Path("/encrypted/v1")
public class EncryptedTriplestoreV1Controller implements EncryptedTriplestoreV1API {
    private static final EncryptedStorageEngine storageEngine = new EncryptedTriplestoreStorageEngine();
    private static final String protocolVersion = "v1";

    @Override
    public Response upload(String triplestoreID, byte[] encryptedNodes, List<String> authorizationHeaders) {
        return EncryptedTriplestoreController.upload(storageEngine, triplestoreID, encryptedNodes, authorizationHeaders);
    }

    @Override
    public Response prepareSearch(String triplestoreID, byte[] trapdoors, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.status(UNAUTHORIZED).build();

        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             ByteArrayInputStream is = new ByteArrayInputStream(trapdoors);
             ObjectInputStream ois = new ObjectInputStream(is)) {
            try (CloseableHttpResponse response = IAMClient.hasReadAccess(httpClient, triplestoreID, accessToken)) {
                if (response.getCode() != OK.getStatusCode())
                    return HTTPUtils.buildResponse(response);
            }

            try (CloseableHttpResponse response = ProxyClient.prepareSearch(httpClient, protocolVersion, storageEngine.search(triplestoreID,
                    (List<String>) ois.readObject()), accessToken)) {
                return HTTPUtils.buildResponse(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response search(String triplestoreID, byte[] trapdoors, List<String> authorizationHeaders) {
        return EncryptedTriplestoreController.search(storageEngine, triplestoreID, trapdoors, authorizationHeaders);
    }

    @Override
    public Response delete(String triplestoreID, List<String> authorizationHeaders) {
        return EncryptedTriplestoreController.delete(storageEngine, triplestoreID, authorizationHeaders);
    }

    @Override
    public Response delete(String triplestoreID, byte[] trapdoors, List<String> authorizationHeaders) {
        return EncryptedTriplestoreController.delete(storageEngine, triplestoreID, trapdoors, authorizationHeaders);
    }

    @Override
    public Response update(String triplestoreID, UpdateForm form, List<String> authorizationHeaders) {
        return EncryptedTriplestoreController.update(storageEngine, triplestoreID, form.getUploads(),
                form.getDeletions(), authorizationHeaders);
    }

}
