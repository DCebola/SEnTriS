package pt.fct.nova.id.srv.application.storage;

import org.apache.jena.graph.Triple;
import java.util.List;

public interface StorageEngine {

    boolean setupStore(String storeID);

    boolean deleteStore(String storeID);

    boolean saveTriple(String storeID, Triple triple);

    List<Triple> getTriples(String storeID);




}
