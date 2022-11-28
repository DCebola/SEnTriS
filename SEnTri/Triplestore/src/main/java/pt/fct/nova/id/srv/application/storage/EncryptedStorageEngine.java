package pt.fct.nova.id.srv.application.storage;

import org.apache.jena.sparql.core.Var;
import pt.fct.nova.id.srv.application.storage.exceptions.StorageEngineException;
import pt.fct.nova.id.srv.application.storage.iri_tables.IRITable;

import java.util.List;
import java.util.Map;

public interface EncryptedStorageEngine {

    void delete(String storeID) throws StorageEngineException;

    void delete(String storeID, List<String> trapdoors) throws StorageEngineException;

    void save(String storeID, Map<String, String> encryptedNodes) throws StorageEngineException;

    List<String> search(String storeID, List<String> trapdoors);

    IRITable search(String storeID, Var var, List<String> trapdoors);

    IRITable search(String storeID, Var var1, Var var2, List<String> trapdoors);

}
