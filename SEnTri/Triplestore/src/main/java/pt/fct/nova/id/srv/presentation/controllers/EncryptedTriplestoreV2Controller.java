package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import pt.fct.nova.id.srv.application.storage.EncryptedStorageEngine;
import pt.fct.nova.id.srv.application.storage.redis.RedisEncryptedStorageEngineV2;
import pt.fct.nova.id.srv.presentation.api.EncryptedTriplestoreAPI;

import java.util.List;
import java.util.Map;


@Path("/encrypted/v2")
public class EncryptedTriplestoreV2Controller implements EncryptedTriplestoreAPI {
    private static final EncryptedStorageEngine storageEngine = new RedisEncryptedStorageEngineV2();
    private static final String protocolVersion = "v2";

    @Override
    public Response upload(String triplestoreID, Map<String, String> encryptedNodes, List<String> authorizationHeaders) {
        System.out.println("upload v2");
        return EncryptedTriplestoreController.upload(storageEngine, triplestoreID, encryptedNodes, authorizationHeaders);
    }

    @Override
    public Response prepareSearch(String triplestoreID, List<String> trapdoors, List<String> authorizationHeaders) {
        System.out.println("prepare search v2");
        return EncryptedTriplestoreController.prepareSearch(storageEngine, protocolVersion, triplestoreID, trapdoors, authorizationHeaders);
    }

    @Override
    public Response search(String triplestoreID, List<String> trapdoors, List<String> authorizationHeaders) {
        System.out.println("search v2");
        return EncryptedTriplestoreController.search(storageEngine, triplestoreID, trapdoors, authorizationHeaders);
    }

    @Override
    public Response delete(String triplestoreID, List<String> authorizationHeaders) {
        System.out.println("delete v2");
        return EncryptedTriplestoreController.delete(storageEngine, triplestoreID, authorizationHeaders);
    }

    @Override
    public Response delete(String triplestoreID, List<String> trapdoors, List<String> authorizationHeaders) {
        System.out.println("delete some v2");
        return EncryptedTriplestoreController.delete(storageEngine, triplestoreID, trapdoors, authorizationHeaders);
    }

    @Override
    public Response swap(String triplestoreID, Map<String, String> values, List<String> authorizationHeaders) {
        System.out.println("swap v2");
        return EncryptedTriplestoreController.swap(storageEngine, triplestoreID, values, authorizationHeaders);
    }
}
