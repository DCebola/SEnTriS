package pt.fct.nova.id.srv.application.protocols;

import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.graph.Triple;
import pt.fct.nova.id.srv.application.InvalidNodeException;
import pt.fct.nova.id.srv.application.crypto.SymmetricCipher;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Protocol1 implements DataOwnerProtocol {
    private final byte[] iv;
    private final SecretKey k1, k2, k3;
    private final Map<String, String> encryptedT;
    private final Map<String, Pair<Integer, byte[]>> keywords;

    public Protocol1() throws NoSuchAlgorithmException {
        this.iv = SymmetricCipher.generateIV();
        this.k1 = SymmetricCipher.generateKey();
        this.k2 = SymmetricCipher.generateKey();
        this.k3 = SymmetricCipher.generateKey();
        this.encryptedT = new HashMap<>();
        this.keywords = new HashMap<>();
    }

    public void exec(String storeID, String password, Iterator<Triple> triples) throws InvalidNodeException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        //TODO: Check database already exists.
        //TODO: Save values
        Triple t;
        String s, p, o;
        while (triples.hasNext()) {
            t = triples.next();
            s = ProtocolUtils.parseNodeIRI(t.getSubject());
            p = ProtocolUtils.parseNodeIRI(t.getSubject());
            o = ProtocolUtils.parseNodeIRI(t.getSubject());
            encodeNode(k1, k2, k3, s, p);
            encodeNode(k1, k2, k3, s, o);
            encodeNode(k1, k2, k3, p, s);
            encodeNode(k1, k2, k3, p, o);
            encodeNode(k1, k2, k3, o, s);
            encodeNode(k1, k2, k3, o, p);
            encodeNode(k1, k2, k3, s, String.format(COMPOUND_KEYWORD, p, o));
            encodeNode(k1, k2, k3, p, String.format(COMPOUND_KEYWORD, s, o));
            encodeNode(k1, k2, k3, o, String.format(COMPOUND_KEYWORD, s, p));
        }
        encryptKeywordInfo();
    }

    private void encryptKeywordInfo() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        for (Map.Entry<String, Pair<Integer, byte[]>> entry : keywords.entrySet()) {
            Pair<Integer, byte[]> value = entry.getValue();
            byte[] st = generateDETLayer(k1, entry.getKey().getBytes(StandardCharsets.UTF_8), iv);
            byte[] ct = generateRNDLayer(k2, ByteBuffer.allocate(4).putInt(value.getLeft()).array());
            encryptedT.put(
                    Base64.getEncoder().encodeToString(st),
                    Base64.getEncoder().encodeToString(ct)
            );
        }
    }

    private void encodeNode(SecretKey k1, SecretKey k2, SecretKey k3, String node, String keyword) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        byte[] st = generateDETLayer(k1, keyword.getBytes(StandardCharsets.UTF_8), getKeywordIV(keyword));
        byte[] ct = generateRNDLayer(k2, generateDETLayer(k3, node.getBytes(StandardCharsets.UTF_8), iv));
        encryptedT.put(
                Base64.getEncoder().encodeToString(st),
                Base64.getEncoder().encodeToString(ct)
        );
    }

    private byte[] generateRNDLayer(SecretKey key, byte[] deterministicCiphertext) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return SymmetricCipher.encrypt(deterministicCiphertext, key);
    }

    private byte[] generateDETLayer(SecretKey key, byte[] plaintext, byte[] iv) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return SymmetricCipher.encrypt(plaintext, key, iv);
    }

    private byte[] getKeywordIV(String keyword) {
        Pair<Integer, byte[]> entry = keywords.get(keyword);
        if (entry == null) {
            entry = new Pair<>(0, SymmetricCipher.incrementIV(iv));
            keywords.put(keyword, entry);
        } else
            keywords.put(keyword, new Pair<>(entry.getLeft() + 1, SymmetricCipher.incrementIV(entry.getRight())));
        return entry.getRight();
    }

}
