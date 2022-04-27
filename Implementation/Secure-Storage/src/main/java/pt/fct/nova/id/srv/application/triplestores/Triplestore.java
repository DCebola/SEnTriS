package pt.fct.nova.id.srv.application.triplestores;

import com.google.gson.Gson;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import pt.fct.nova.id.srv.application.storage.QueryEngine;
import pt.fct.nova.id.srv.application.storage.StorageEngine;
import pt.fct.nova.id.srv.application.storage.clients.StorageClient;
import pt.fct.nova.id.srv.application.storage.clients.redis.RedisClient;

import java.util.Iterator;
import java.util.Set;

public class Triplestore implements StorageEngine, QueryEngine {

    private final Gson gson = new Gson();
    private final StorageClient db = new RedisClient();

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
