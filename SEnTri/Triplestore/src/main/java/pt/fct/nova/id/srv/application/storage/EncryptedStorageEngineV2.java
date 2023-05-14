package pt.fct.nova.id.srv.application.storage;

import java.math.BigInteger;
import java.util.List;

public interface EncryptedStorageEngineV2 extends EncryptedStorageEngine{

    List<String> maskedSearch(String triplestoreID, List<String> trapdoors, BigInteger mask);

}
