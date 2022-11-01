package pt.fct.nova.id.srv.application.protocols;

import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.graph.Triple;
import pt.fct.nova.id.srv.application.clients.TriplestoreClient;
import pt.fct.nova.id.srv.application.clients.exception.TriplestoreCreateException;
import pt.fct.nova.id.srv.application.clients.exception.TriplestoreUploadException;
import pt.fct.nova.id.srv.application.crypto.SymmetricCipher;
import pt.fct.nova.id.srv.application.protocols.exceptions.InvalidNodeException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Protocol1 implements WriteProtocol {

    private final int batchLength;
    private final String storeID;

    private boolean toBeCreated;
    private final byte[] iv;
    private final SecretKey k1, k2, k3;
    private final Map<String, String> encryptedT;
    private final Map<String, Pair<Integer, byte[]>> keywords;

    public Protocol1(int batchLength, String storeID, Map<String, Pair<Integer, byte[]>> keywords) throws NoSuchAlgorithmException {
        this.storeID = storeID;
        this.iv = SymmetricCipher.generateIV();
        this.k1 = SymmetricCipher.generateKey();
        this.k2 = SymmetricCipher.generateKey();
        this.k3 = SymmetricCipher.generateKey();
        this.encryptedT = new HashMap<>();
        this.keywords = keywords;
        this.batchLength = batchLength;
        this.toBeCreated = false;
    }

    public Protocol1(String storeID, Map<String, Pair<Integer, byte[]>> keywords) throws NoSuchAlgorithmException {
        this.storeID = storeID;
        this.iv = SymmetricCipher.generateIV();
        this.k1 = SymmetricCipher.generateKey();
        this.k2 = SymmetricCipher.generateKey();
        this.k3 = SymmetricCipher.generateKey();
        this.encryptedT = new HashMap<>();
        this.keywords = keywords;
        this.batchLength = -1;
        this.toBeCreated = false;

    }

    public Protocol1(String storeID) throws NoSuchAlgorithmException {
        this.storeID = storeID;
        this.iv = SymmetricCipher.generateIV();
        this.k1 = SymmetricCipher.generateKey();
        this.k2 = SymmetricCipher.generateKey();
        this.k3 = SymmetricCipher.generateKey();
        this.encryptedT = new HashMap<>();
        this.keywords = new HashMap<>();
        this.batchLength = -1;
        this.toBeCreated = true;
    }

    public int getBatchLength() {
        return batchLength;
    }

    public String getStoreID() {
        return storeID;
    }

    public boolean isToBeCreated() {
        return toBeCreated;
    }

    public byte[] getIv() {
        return iv;
    }

    public SecretKey getK1() {
        return k1;
    }

    public SecretKey getK2() {
        return k2;
    }

    public SecretKey getK3() {
        return k3;
    }

    public Map<String, Pair<Integer, byte[]>> getKeywords() {
        return keywords;
    }

    public void exec(List<Triple> triples) throws InvalidNodeException, InvalidAlgorithmParameterException,
            NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException,
            InvalidKeyException, RuntimeException, TriplestoreCreateException, UnsupportedEncodingException, TriplestoreUploadException {
        String s, p, o;
        int i = 0;
        for(Triple t: triples) {
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
            if (batchLength > 0 && i == batchLength) {
                save();
                i = 0;
            }
            i++;
        }
        save();
        encryptKeywordInfo();
    }

    private void save() throws UnsupportedEncodingException, TriplestoreCreateException, TriplestoreUploadException {
        if (toBeCreated) {
            TriplestoreClient.create(storeID, encryptedT);
            encryptedT.clear();
            toBeCreated = false;
        } else {
            TriplestoreClient.upload(storeID, encryptedT);
            encryptedT.clear();
        }
    }

    private void encryptKeywordInfo() throws InvalidAlgorithmParameterException,
            NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException,
            BadPaddingException, InvalidKeyException, TriplestoreCreateException, UnsupportedEncodingException,
            TriplestoreUploadException {
        int i = 0;
        for (Map.Entry<String, Pair<Integer, byte[]>> entry : keywords.entrySet()) {
            Pair<Integer, byte[]> value = entry.getValue();
            byte[] st = generateDETLayer(k1, entry.getKey().getBytes(StandardCharsets.UTF_8), iv);
            byte[] ct = generateRNDLayer(k2, ByteBuffer.allocate(4).putInt(value.getLeft()).array());
            encryptedT.put(
                    Base64.getEncoder().encodeToString(st),
                    Base64.getEncoder().encodeToString(ct)
            );
            if (batchLength > 0 && i == batchLength) {
                save();
                i = 0;
            }
            i++;
        }
        save();
    }

    private void encodeNode(SecretKey k1, SecretKey k2, SecretKey k3, String node, String keyword)
            throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException,
            NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
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
