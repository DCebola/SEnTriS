package pt.fct.nova.id.srv.application.triplestores;

import pt.fct.nova.id.srv.application.query.QueryEngine;
import pt.fct.nova.id.srv.application.storage.StorageEngine;

public class TriplestoreFactory {

    public static Triplestore createSimpleTriplestore(StorageEngine storageEngine, QueryEngine queryEngine) {
        return new SimpleTriplestore(storageEngine, queryEngine);
    }

}
