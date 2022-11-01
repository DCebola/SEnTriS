package pt.fct.nova.id.srv.application.protocols;

import org.apache.jena.graph.Triple;
import pt.fct.nova.id.srv.application.clients.exception.TriplestoreCreateException;
import pt.fct.nova.id.srv.application.clients.exception.TriplestoreUploadException;
import pt.fct.nova.id.srv.application.protocols.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.protocols.exceptions.UnknownProtocolException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public interface WriteProtocol {
    String KEYWORD_SEPARATOR = System.getenv("KEYWORD_SEPARATOR");
    String COMPOUND_KEYWORD = "%s".concat(KEYWORD_SEPARATOR).concat("%s");

    void exec(List<Triple> triples) throws InvalidNodeException, UnknownProtocolException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, RuntimeException, TriplestoreCreateException, UnsupportedEncodingException, TriplestoreUploadException;

}
