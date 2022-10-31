package pt.fct.nova.id.srv.application.protocols;


import org.apache.jena.graph.Triple;
import java.util.Iterator;

public class Protocol2 implements DataOwnerProtocol {


    public void exec(String storeID, String password, Iterator<Triple> triples) {
        //TODO: Check database already exists.
        //TODO: Get master key or Generate & Store master key,
        //TODO: Retrieve keys or Generate & Store in keystore, under Enc(masterKey, storeID).
        //TODO: Protocol 2.
        //TODO: Save data in Redis, encrypted under master key.
    }


    @Override
    public void uploadData(String storeID, String password, Iterator<Triple> triples) {
        //TODO: Get master key or Generate & Store master key,
        //TODO: Retrieve keys or Generate & Store in keystore, under Enc(masterKey, storeID).
        //TODO: Retrieve data associated to store.
        //TODO: Continue protocol 2.
        //TODO: Save data in Redis.
    }
}
