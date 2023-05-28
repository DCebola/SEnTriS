package pt.fct.nova.id.srv.application.storage;

import java.math.BigInteger;
import java.util.List;

public interface EncryptedStorageEngineV2 extends EncryptedStorageEngine{

    List<byte[]> maskedSearch(String triplestoreID, List<byte[]> trapdoors, BigInteger mask);

}
