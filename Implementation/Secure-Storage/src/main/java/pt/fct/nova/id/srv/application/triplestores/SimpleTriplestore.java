package pt.fct.nova.id.srv.application.triplestores;

import org.apache.jena.graph.Triple;
import pt.fct.nova.id.srv.application.query.QueryEngine;
import pt.fct.nova.id.srv.application.storage.StorageEngine;

import java.util.Iterator;

public class SimpleTriplestore implements Triplestore {

    private final StorageEngine storageEngine;
    private final QueryEngine queryEngine;

    public SimpleTriplestore(StorageEngine storageEngine, QueryEngine queryEngine) {
        this.storageEngine = storageEngine;
        this.queryEngine = queryEngine;
    }

    @Override
    public boolean createDataset(String storeID, Iterator<Triple> triples) {
        boolean success = storageEngine.setupStore(storeID);
        if (!success)
            return false;
        while (triples.hasNext()) {
            success = storageEngine.saveTriple(storeID, triples.next());
            if (!success) {
                storageEngine.deleteStore(storeID);
                return false;
            }
        }
        return true;
    }

    @Override
    public Iterator<Triple> getDataset(String storeID) {
        return storageEngine.getTriples(storeID);
    }


}
