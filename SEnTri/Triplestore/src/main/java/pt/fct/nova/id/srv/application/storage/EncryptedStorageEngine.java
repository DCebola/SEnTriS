package pt.fct.nova.id.srv.application.storage;

import org.apache.jena.sparql.core.Var;
import pt.fct.nova.id.srv.application.storage.iri_tables.IRITable;

import java.util.List;
import java.util.Map;

public interface EncryptedStorageEngine {

    void delete(String storeID);

    void delete(String storeID, List<String> trapdoors);

    void save(String storeID, Map<String, String> encryptedNodes);

    List<String> search(String storeID, List<String> trapdoors);

    IRITable search(String storeID, Var var, List<String> trapdoors);

    IRITable search(String storeID, Var var1, Var var2, List<String> trapdoors);

}
