package pt.fct.nova.id.srv.application.triplestores;

import org.apache.jena.graph.Node;
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
    public void createDataset(String storeID, Iterator<Triple> triples) {
        while (triples.hasNext()) {
            Triple t = triples.next();
            saveTriple(t.getSubject(), t.getPredicate(), t.getObject());
        }
    }

    @Override
    public Iterator<Triple> getDataset(String storeID) {
        return storageEngine.getTriples(storeID);
    }

    private void saveTriple(Node subject, Node predicate, Node object) {
        //TODO: Upload Node + IRI
        //TODO: Upload Indexes
    }
}
