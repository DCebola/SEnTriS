package pt.fct.nova.id.srv.application.storage;

import org.apache.jena.graph.Triple;
import java.util.List;
import java.util.Map;

public interface StorageEngine {

    boolean setupStore(String storeID, Map<String, String> namespaces);

    boolean deleteStore(String storeID);

    boolean saveTriple(String storeID, Triple triple);

    List<Triple> getTriples(String storeID);

    Map<String, String> getNamespaces(String storeID);
}
