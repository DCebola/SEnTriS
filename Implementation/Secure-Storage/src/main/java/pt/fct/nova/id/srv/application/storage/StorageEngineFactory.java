package pt.fct.nova.id.srv.application.storage;

import pt.fct.nova.id.srv.application.storage.redis.RStorageEngine;

public class StorageEngineFactory {
    public static StorageEngine createNewStorageEngine(String type) {
        StorageEngine eng;
        return new RStorageEngine();
    }
}
