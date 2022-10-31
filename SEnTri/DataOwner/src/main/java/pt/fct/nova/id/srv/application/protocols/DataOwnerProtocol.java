package pt.fct.nova.id.srv.application.protocols;

import org.apache.jena.graph.Triple;
import pt.fct.nova.id.srv.application.InvalidNodeException;
import pt.fct.nova.id.srv.application.UnknownProtocolException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

public interface DataOwnerProtocol {
    String KEYWORD_SEPARATOR = System.getenv("KEYWORD_SEPARATOR");
    String COMPOUND_KEYWORD = "%s".concat(KEYWORD_SEPARATOR).concat("%s");

    void exec(String storeID, String password, Iterator<Triple> triples) throws InvalidNodeException, UnknownProtocolException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException;

}
