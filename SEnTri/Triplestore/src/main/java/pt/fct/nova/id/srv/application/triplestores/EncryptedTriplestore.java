package pt.fct.nova.id.srv.application.triplestores;

import pt.fct.nova.id.srv.application.storage.exceptions.StoreAlreadyExistsException;
import pt.fct.nova.id.srv.application.storage.exceptions.StoreNotFoundException;

import java.util.List;
import java.util.Map;

public interface EncryptedTriplestore {
    void createDataset(String storeID, Map<String, String> encryptedNodes) throws StoreAlreadyExistsException;
    void uploadData(String storeID, Map<String, String> encryptedNodes) throws StoreNotFoundException;
    void delete(String storeID);
    void delete(String storeID, List<String> trapdoors);
    List<String> search(String storeID, List<String> trapdoors);
}
