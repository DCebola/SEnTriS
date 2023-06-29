package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.core.Response;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import pt.fct.nova.id.srv.application.clients.HTTPClient;
import pt.fct.nova.id.srv.application.clients.HTTPUtils;
import pt.fct.nova.id.srv.application.clients.IAMClient;
import pt.fct.nova.id.srv.application.storage.EncryptedStorageEngine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static jakarta.ws.rs.core.Response.Status.*;
import static pt.fct.nova.id.srv.application.clients.HTTPUtils.extractAccessToken;
import static pt.fct.nova.id.srv.presentation.controllers.TriplestoreController.*;


public class EncryptedTriplestoreController {
    private static final String SUCCESSFUL_UPDATE = "Successful update.";
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder();

    public static Response upload(EncryptedStorageEngine storageEngine, String triplestoreID, byte[] encryptedNodes, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.hasWriteAccess(httpClient, triplestoreID, accessToken);
             ByteArrayInputStream bis = new ByteArrayInputStream(encryptedNodes);
             ObjectInputStream ois = new ObjectInputStream(bis)) {

            if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                return HTTPUtils.buildResponse(response);
            return Response.ok(base64Encoder.encodeToString(storageEngine.commitUpload(triplestoreID, (Map<byte[], byte[]>) ois.readObject()))).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    public static Response search(EncryptedStorageEngine storageEngine, String triplestoreID, byte[] trapdoors, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.hasReadAccess(httpClient, triplestoreID, accessToken);
             ByteArrayInputStream bis = new ByteArrayInputStream(trapdoors);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                return HTTPUtils.buildResponse(response);
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                 ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                oos.writeObject(storageEngine.search(triplestoreID, (List<byte[]>) ois.readObject()));
                return Response.ok(base64Encoder.encodeToString(bos.toByteArray())).build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }


    public static Response delete(EncryptedStorageEngine storageEngine, String triplestoreID, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.hasOwnerAccess(httpClient, triplestoreID, accessToken)) {
            if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                return HTTPUtils.buildResponse(response);
            storageEngine.delete(triplestoreID);
            return Response.ok(SUCCESSFUL_DELETION).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    public static Response delete(EncryptedStorageEngine storageEngine, String triplestoreID, byte[] trapdoors, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.hasWriteAccess(httpClient, triplestoreID, accessToken);
             ByteArrayInputStream bis = new ByteArrayInputStream(trapdoors);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                return HTTPUtils.buildResponse(response);
            return Response.ok(base64Encoder.encodeToString(storageEngine.commitDelete(triplestoreID, (Set<byte[]>) ois.readObject()))).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    public static Response update(EncryptedStorageEngine storageEngine, String triplestoreID,
                                  byte[] uploads, byte[] deletions, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.hasWriteAccess(httpClient, triplestoreID, accessToken);
             ByteArrayInputStream uploads_bis = new ByteArrayInputStream(uploads);
             ObjectInputStream uploads_ois = new ObjectInputStream(uploads_bis);
             ByteArrayInputStream deletions_bis = new ByteArrayInputStream(deletions);
             ObjectInputStream deletions_ois = new ObjectInputStream(deletions_bis)) {
            if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                return HTTPUtils.buildResponse(response);
            storageEngine.update(triplestoreID, (List<byte[]>) uploads_ois.readObject(), (List<byte[]>) deletions_ois.readObject());
            return Response.ok(SUCCESSFUL_UPDATE).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
