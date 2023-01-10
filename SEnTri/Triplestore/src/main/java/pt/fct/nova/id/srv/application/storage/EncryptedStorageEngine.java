package pt.fct.nova.id.srv.application.storage;

import java.util.List;
import java.util.Map;

public interface EncryptedStorageEngine {

    void delete(String triplestoreID);

    void delete(String triplestoreID, List<String> trapdoors);

    void save(String triplestoreID, Map<String, String> encryptedNodes);

    void swap(String triplestoreID, Map<String, String> values);

    List<String> search(String triplestoreID, List<String> trapdoors);

}
