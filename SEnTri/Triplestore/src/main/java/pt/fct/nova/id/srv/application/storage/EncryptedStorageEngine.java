package pt.fct.nova.id.srv.application.storage;

import pt.fct.nova.id.srv.application.storage.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.storage.exceptions.StorageEngineException;
import pt.fct.nova.id.srv.application.storage.exceptions.StoreAlreadyExistsException;
import pt.fct.nova.id.srv.application.storage.exceptions.StoreNotFoundException;

import java.util.Map;

public interface EncryptedStorageEngine {

    void setupStore(String storeID) throws StorageEngineException;

    void deleteStore(String storeID) throws StorageEngineException;

    void save(String storeID, Map<String, String> encryptedNodes) throws StorageEngineException;

    void checkID(String storeID) throws StoreAlreadyExistsException, StoreNotFoundException;
}
