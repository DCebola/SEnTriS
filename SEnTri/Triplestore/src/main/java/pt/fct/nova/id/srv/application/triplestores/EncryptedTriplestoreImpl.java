package pt.fct.nova.id.srv.application.triplestores;

import pt.fct.nova.id.srv.application.storage.EncryptedStorageEngine;
import pt.fct.nova.id.srv.application.storage.exceptions.StorageEngineException;
import pt.fct.nova.id.srv.application.storage.exceptions.StoreAlreadyExistsException;
import pt.fct.nova.id.srv.application.storage.exceptions.StoreNotFoundException;

import java.util.List;
import java.util.Map;

public class EncryptedTriplestoreImpl implements EncryptedTriplestore {

    private final EncryptedStorageEngine storageEngine;

    public EncryptedTriplestoreImpl(EncryptedStorageEngine storageEngine) {
        this.storageEngine = storageEngine;
    }

    @Override
    public void createDataset(String storeID, Map<String, String> encryptedNodes) throws StoreAlreadyExistsException {
        verifyStoreDoesNotExist(storeID);
        storageEngine.setup(storeID);
        saveNodes(storeID, encryptedNodes);

    }

    private void saveNodes(String storeID, Map<String, String> encryptedNodes) {
        try {
            if (encryptedNodes != null) {
                storageEngine.save(storeID, encryptedNodes);
            }
        } catch (StorageEngineException e) {
            storageEngine.delete(storeID);
        }
    }

    @Override
    public void uploadData(String storeID, Map<String, String> encryptedNodes) throws StoreNotFoundException {
        verifyStoreExists(storeID);
        saveNodes(storeID, encryptedNodes);
    }

    @Override
    public void delete(String storeID) {
        verifyStoreExists(storeID);
        storageEngine.delete(storeID);
    }

    @Override
    public void delete(String storeID, List<String> trapdoors) {
        verifyStoreExists(storeID);
        storageEngine.delete(storeID, trapdoors);
    }

    @Override
    public List<String> search(String storeID, List<String> trapdoors) {
        verifyStoreExists(storeID);
        return storageEngine.search(storeID, trapdoors);
    }

    private void verifyStoreExists(String storeID) throws StoreNotFoundException {
        try {
            storageEngine.checkID(storeID);
        } catch (StoreAlreadyExistsException ignored) {
        }
    }

    private void verifyStoreDoesNotExist(String storeID) throws StoreAlreadyExistsException {
        try {
            storageEngine.checkID(storeID);
        } catch (StoreNotFoundException ignored) {
        }
    }
}
