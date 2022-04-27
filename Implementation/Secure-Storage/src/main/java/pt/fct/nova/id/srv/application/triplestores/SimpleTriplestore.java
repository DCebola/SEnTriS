package pt.fct.nova.id.srv.application.triplestores;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import pt.fct.nova.id.srv.application.storage.clients.StorageClient;

import java.util.Iterator;

public class SimpleTriplestore implements Triplestore {

    private final StorageClient db;

    public SimpleTriplestore(StorageClient storageClient) {
        db = storageClient;
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
        return db.getTriples(storeID);
    }

    private void saveTriple(Node subject, Node predicate, Node object) {
        //TODO: Upload Node + URI
        //TODO: Upload Indexes
    }
}
