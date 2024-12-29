package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import pt.fct.nova.id.srv.application.clients.HTTPClient;
import pt.fct.nova.id.srv.application.clients.HTTPUtils;
import pt.fct.nova.id.srv.application.clients.IAMClient;
import pt.fct.nova.id.srv.application.clients.ProxyClient;
import pt.fct.nova.id.srv.application.storage.EncryptedStorageEngineV2;
import pt.fct.nova.id.srv.application.storage.redis.EncryptedTriplestoreStorageEngineV2;
import pt.fct.nova.id.srv.presentation.apis.EncryptedTriplestoreV2API;
import pt.fct.nova.id.srv.presentation.dtos.PrepareSearchV2Form;
import pt.fct.nova.id.srv.presentation.dtos.UpdateForm;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.util.List;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.OK;
import static pt.fct.nova.id.srv.application.clients.HTTPUtils.extractAccessToken;
import static pt.fct.nova.id.srv.presentation.controllers.TriplestoreController.INTERNAL_ERROR;
import static pt.fct.nova.id.srv.presentation.controllers.TriplestoreController.NO_ACCESS_TOKEN;


@Path("/encrypted/v2")
public class EncryptedTriplestoreV2Controller implements EncryptedTriplestoreV2API {
    private static final EncryptedStorageEngineV2 storageEngine = new EncryptedTriplestoreStorageEngineV2();
    private static final String protocolVersion = "v2";

    @Override
    public Response upload(String triplestoreID, byte[] encryptedNodes, List<String> authorizationHeaders) {
        return EncryptedTriplestoreController.upload(storageEngine, triplestoreID, encryptedNodes, authorizationHeaders);
    }

    @Override
    public Response prepareMaskedSearch(String triplestoreID, PrepareSearchV2Form form, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             ByteArrayInputStream trapdoors_is = new ByteArrayInputStream(form.getTrapdoors());
             ObjectInputStream trapdoors_ois = new ObjectInputStream(trapdoors_is)) {
            try (CloseableHttpResponse response = IAMClient.hasReadAccess(httpClient, triplestoreID, accessToken)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HTTPUtils.buildResponse(response);
            }

            try (CloseableHttpResponse response = ProxyClient.prepareSearch(httpClient, protocolVersion,
                    storageEngine.maskedSearch(triplestoreID, (List<String>) trapdoors_ois.readObject(), new BigInteger(form.getMask()), new BigInteger(form.getN())), accessToken)) {
                return HTTPUtils.buildResponse(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
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
