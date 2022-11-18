package pt.fct.nova.id.srv.application.protocols;


import org.apache.jena.graph.Triple;
import java.util.List;

public class Protocol2 implements EncryptionProtocol {
    public void exec(List<Triple> triples) {
        //TODO: Check database already exists.
        //TODO: Get master key or Generate & Store master key,
        //TODO: Retrieve keys or Generate & Store in keystore, under Enc(masterKey, storeID).
        //TODO: Protocol 2.
        //TODO: Save data in Redis, encrypted under master key.
    }
}
