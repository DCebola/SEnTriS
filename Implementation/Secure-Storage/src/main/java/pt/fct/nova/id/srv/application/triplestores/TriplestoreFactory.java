package pt.fct.nova.id.srv.application.triplestores;

import pt.fct.nova.id.srv.application.storage.clients.StorageClient;

public class TriplestoreFactory {

    public static Triplestore createSimpleTriplestore(StorageClient storageClient) {
        return new SimpleTriplestore(storageClient);
    }

}
