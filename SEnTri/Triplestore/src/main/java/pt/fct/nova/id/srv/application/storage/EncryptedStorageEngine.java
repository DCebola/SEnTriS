package pt.fct.nova.id.srv.application.storage;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface EncryptedStorageEngine {

    void delete(String triplestoreID);

    void delete(String triplestoreID, Set<byte[]> trapdoors);

    void save(String triplestoreID, Map<byte[], byte[]> encryptedNodes);

    List<byte[]> search(String triplestoreID, List<byte[]> trapdoors);

}
