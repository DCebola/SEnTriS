package pt.fct.nova.id.srv.application.protocols;


import org.apache.jena.graph.Triple;
import pt.fct.nova.id.srv.application.protocols.exceptions.InvalidNodeException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

public class Protocol2 implements EncryptionProtocol {

    public void exec(List<Triple> triples, boolean schema) {
        //TODO: Check database already exists.
        //TODO: Get master key or Generate & Store master key,
        //TODO: Retrieve keys or Generate & Store in keystore, under Enc(masterKey, storeID).
        //TODO: Protocol 2.
        //TODO: Save data in Redis, encrypted under master key.
    }
}
