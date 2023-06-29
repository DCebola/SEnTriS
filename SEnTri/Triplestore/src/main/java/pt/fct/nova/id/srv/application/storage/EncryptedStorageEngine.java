package pt.fct.nova.id.srv.application.storage;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface EncryptedStorageEngine {

    void delete(String triplestoreID);

    byte[] commitDelete(String triplestoreID, Set<byte[]> trapdoors);

    byte[] commitUpload(String triplestoreID, Map<byte[], byte[]> encryptedNodes);

    void update(String triplestoreID, List<byte[]> uploads, List<byte[]> deletions);

    List<byte[]> search(String triplestoreID, List<byte[]> trapdoors);

}
