package pt.fct.nova.id.srv.application.storage;

import org.apache.jena.sparql.core.Var;
import pt.fct.nova.id.srv.application.storage.exceptions.StorageEngineException;
import pt.fct.nova.id.srv.application.storage.exceptions.StoreAlreadyExistsException;
import pt.fct.nova.id.srv.application.storage.exceptions.StoreNotFoundException;
import pt.fct.nova.id.srv.application.storage.iri_tables.IRITable;

import java.util.List;
import java.util.Map;

public interface EncryptedStorageEngine {

    void setupStore(String storeID) throws StorageEngineException;

    void deleteStore(String storeID) throws StorageEngineException;

    void save(String storeID, Map<String, String> encryptedNodes) throws StorageEngineException;

    void checkID(String storeID) throws StoreAlreadyExistsException, StoreNotFoundException;

    IRITable search(String storeID, Var var, List<String> trapdoors);

    IRITable search(String storeID, Var var1, Var var2, List<String> trapdoors);

}
