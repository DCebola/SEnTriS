package pt.fct.nova.id.srv.application.protocols;

import org.apache.jena.graph.Triple;
import pt.fct.nova.id.srv.application.protocols.exceptions.InvalidNodeException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

import static pt.fct.nova.id.srv.presentation.controllers.ParsingUtils.BASIC_SEPARATOR;

public interface EncryptionProtocol {

    String TRIPLE_KEYWORD = "%s".concat(BASIC_SEPARATOR).concat("%s").concat(BASIC_SEPARATOR).concat("%s");
    String COMPOUND_KEYWORD = "%s".concat(BASIC_SEPARATOR).concat("%s");
    String KEYWORD_FORMAT = "%s".concat(BASIC_SEPARATOR).concat("%s");

    void exec(Set<Triple> triples, boolean schema) throws InvalidNodeException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException;

}
