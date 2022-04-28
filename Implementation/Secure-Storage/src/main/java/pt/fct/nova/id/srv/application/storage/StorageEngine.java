package pt.fct.nova.id.srv.application.storage;

import org.apache.jena.graph.Triple;
import java.util.Iterator;

public interface StorageEngine {


    boolean setupStore(String storeID);

    boolean deleteStore(String storeID);

    boolean saveTriple(String storeID, Triple triple);

    Iterator<Triple> getTriples(String storeID);




}
