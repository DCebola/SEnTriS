package pt.fct.nova.id.srv.application.protocols;

import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.graph.Triple;

import pt.fct.nova.id.srv.application.crypto.SymmetricCipher;
import pt.fct.nova.id.srv.application.protocols.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.query.jobs.VariablesPattern;
import pt.fct.nova.id.srv.presentation.controllers.ClientUtils;

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

import static org.apache.commons.codec.binary.Base64.*;

public class Protocol1 implements EncryptionProtocol {

    private final String storeID;
    private final byte[] iv;
    private final SecretKey k1, k2, k3;
    private final Map<String, String> encryptedT;
    private final Map<String, Pair<Integer, byte[]>> keywords;

    public Protocol1(String storeID, SecretKey k1, SecretKey k2, SecretKey k3, byte[] iv) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        this.iv = iv;
        this.k1 = k1;
        this.k2 = k2;
        this.k3 = k3;
        this.storeID = encodeBase64URLSafeString(generateDETLayer(k1, storeID.getBytes(StandardCharsets.UTF_8), iv));
        this.encryptedT = new HashMap<>();
        this.keywords = new HashMap<>();

    }

    public Protocol1(String storeID) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        this.iv = SymmetricCipher.generateIV();
        this.k1 = SymmetricCipher.generateKey();
        this.k2 = SymmetricCipher.generateKey();
        this.k3 = SymmetricCipher.generateKey();
        this.storeID = encodeBase64URLSafeString(generateDETLayer(k1, storeID.getBytes(StandardCharsets.UTF_8), iv));
        this.encryptedT = new HashMap<>();
        this.keywords = new HashMap<>();
    }

    public String getStoreID() {
        return storeID;
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

    public Map<String, String> getEncryptedT() {
        return encryptedT;
    }

    @Override
    public void exec(List<Triple> triples) throws InvalidNodeException, InvalidAlgorithmParameterException,
            NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException,
            InvalidKeyException, RuntimeException {
        encryptTriples(triples);
        encryptKeywordInfo();
    }


    private void encryptTriples(List<Triple> triples) throws InvalidNodeException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        String s, p, o;
        for (Triple t : triples) {
            s = ClientUtils.parseNodeIRI(t.getSubject());
            p = ClientUtils.parseNodeIRI(t.getSubject());
            o = ClientUtils.parseNodeIRI(t.getSubject());
            encodeNode(k1, k2, k3, s, VariablesPattern.P, p);
            encodeNode(k1, k2, k3, s, VariablesPattern.O, o);
            encodeNode(k1, k2, k3, p, VariablesPattern.S, s);
            encodeNode(k1, k2, k3, p, VariablesPattern.O, o);
            encodeNode(k1, k2, k3, o, VariablesPattern.S, s);
            encodeNode(k1, k2, k3, o, VariablesPattern.P, p);
            encodeNode(k1, k2, k3, s, VariablesPattern.PO, String.format(COMPOUND_KEYWORD, p, o));
            encodeNode(k1, k2, k3, p, VariablesPattern.SO, String.format(COMPOUND_KEYWORD, s, o));
            encodeNode(k1, k2, k3, o, VariablesPattern.SP, String.format(COMPOUND_KEYWORD, s, p));
        }
    }

    private void encryptKeywordInfo() throws InvalidAlgorithmParameterException,
            NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException,
            BadPaddingException, InvalidKeyException {
        for (Map.Entry<String, Pair<Integer, byte[]>> entry : keywords.entrySet()) {
            Pair<Integer, byte[]> value = entry.getValue();
            byte[] st = generateDETLayer(k1, entry.getKey().getBytes(StandardCharsets.UTF_8), iv);
            byte[] ct = generateRNDLayer(k2, ByteBuffer.allocate(Integer.BYTES).putInt(value.getLeft()).array());
            encryptedT.put(
                    encodeBase64URLSafeString(st),
                    encodeBase64URLSafeString(ct)
            );
        }
    }

    public Map<String, String> generateKeywordTrapdoorMap(List<Triple> triples) throws InvalidNodeException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        HashMap<String, String> res = new HashMap<>();
        Set<String> skip = new HashSet<>();
        String s, p, o, po, so, sp;
        for (Triple t : triples) {
            s = ClientUtils.parseNodeIRI(t.getSubject());
            p = ClientUtils.parseNodeIRI(t.getPredicate());
            o = ClientUtils.parseNodeIRI(t.getObject());
            po = String.format(COMPOUND_KEYWORD, p, o);
            so = String.format(COMPOUND_KEYWORD, s, o);
            sp = String.format(COMPOUND_KEYWORD, s, p);
            generateKeywordTrapdoor(res, skip, VariablesPattern.S, s);
            generateKeywordTrapdoor(res, skip, VariablesPattern.P, p);
            generateKeywordTrapdoor(res, skip, VariablesPattern.O, o);
            generateKeywordTrapdoor(res, skip, VariablesPattern.PO, po);
            generateKeywordTrapdoor(res, skip, VariablesPattern.SO, so);
            generateKeywordTrapdoor(res, skip, VariablesPattern.SP, sp);
        }
        return res;
    }

    private void encodeNode(SecretKey k1, SecretKey k2, SecretKey k3, String node, VariablesPattern pattern, String keyword)
            throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException,
            NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        keyword = String.format(KEYWORD_FORMAT, pattern, keyword);
        byte[] st = generateDETLayer(k1, keyword.getBytes(StandardCharsets.UTF_8), getKeywordIV(keyword));
        byte[] ct = generateRNDLayer(k2, generateDETLayer(k3, node.getBytes(StandardCharsets.UTF_8), iv));
        encryptedT.put(
                encodeBase64URLSafeString(st),
                encodeBase64URLSafeString(ct)
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


    private void generateKeywordTrapdoor(Map<String, String> trapdoors, Set<String> skip, VariablesPattern pattern, String keyword) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        keyword = String.format(KEYWORD_FORMAT, pattern, keyword);
        if (!skip.contains(keyword)) {
            trapdoors.put(keyword, generateTrapdoor(keyword));
            skip.add(keyword);
        }
    }

    public Map<String, Pair<Integer, byte[]>> generateKeywordIVMap(List<String> keywords, List<String> keywordsTotals) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        int max = -1;
        int total;
        Map<Integer, byte[]> generatedIvs = new HashMap<>();
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        Map<String, Pair<Integer, byte[]>> res = new HashMap<>();
        int i = 0;
        for (String encTotal : keywordsTotals) {
            total = buffer.put(SymmetricCipher.decrypt(k2, decodeBase64(encTotal))).rewind().getInt();
            buffer.reset();
            if (total > max) {
                for (int j = 0; j < total - max; j++)
                    generatedIvs.put(total - j, SymmetricCipher.incrementIV(iv));
                max = total;
            }
            res.put(keywords.get(i), new Pair<>(total, generatedIvs.get(total)));
            i++;
        }
        return res;
    }

    private String generateTrapdoor(String keyword) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        return encodeBase64URLSafeString(generateDETLayer(k1, keyword.getBytes(StandardCharsets.UTF_8), iv));
    }

    public void updateKeywords(Map<String, Pair<Integer, byte[]>> values) {
        keywords.putAll(values);
    }
}
