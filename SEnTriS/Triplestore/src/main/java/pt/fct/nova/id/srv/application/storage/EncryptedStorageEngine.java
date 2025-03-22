package pt.fct.nova.id.srv.application.storage;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface EncryptedStorageEngine {

    void delete(String triplestoreID);

    String commitDelete(String triplestoreID, Set<String> trapdoors);

    String commitUpload(String triplestoreID, Map<String, String> encryptedNodes);

    void update(List<String> uploads, List<String> deletions);

    List<byte[]> search(String triplestoreID, List<String> trapdoors);

}
