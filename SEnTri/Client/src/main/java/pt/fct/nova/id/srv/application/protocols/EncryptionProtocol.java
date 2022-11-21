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

public interface EncryptionProtocol {
    String KEYWORD_SEPARATOR = System.getenv("BASIC_SEPARATOR");
    String COMPOUND_KEYWORD = "%s".concat(KEYWORD_SEPARATOR).concat("%s");
    void exec(List<Triple> triples) throws InvalidNodeException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException;
}
