package pt.fct.nova.id.srv.application.protocols;

import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import pt.fct.nova.id.srv.application.crypto.SymmetricCipher;
import pt.fct.nova.id.srv.application.protocols.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.query.jobs.VariablesPattern;
import pt.fct.nova.id.srv.presentation.controllers.ParsingUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static org.apache.commons.codec.binary.Base64.*;

public class Protocol1 implements EncryptionProtocol {
    private final byte[] iv;
    private final SecretKey k1, k2, k3;
    private final Map<String, String> encryptedT;
    private final Map<String, Pair<Integer, byte[]>> keywords;
    private final Base64.Decoder base64Decoder;
    private final Base64.Encoder base64Encoder;

    public Protocol1(SecretKey k1, SecretKey k2, SecretKey k3, byte[] iv) {
        this.iv = iv;
        this.k1 = k1;
        this.k2 = k2;
        this.k3 = k3;
        this.encryptedT = new HashMap<>();
        this.keywords = new HashMap<>();
        this.base64Decoder = Base64.getUrlDecoder();
        this.base64Encoder = Base64.getUrlEncoder();
    }

    public Protocol1() throws NoSuchAlgorithmException {
        this.iv = SymmetricCipher.generateIV();
        this.k1 = SymmetricCipher.generateKey();
        this.k2 = SymmetricCipher.generateKey();
        this.k3 = SymmetricCipher.generateKey();
        this.encryptedT = new HashMap<>();
        this.keywords = new HashMap<>();
        this.base64Decoder = Base64.getUrlDecoder();
        this.base64Encoder = Base64.getUrlEncoder();
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
        Node s, p, o;
        String s_iri, p_iri, o_iri, s_keyword, p_keyword, o_keyword;
        for (Triple t : triples) {
            s = t.getSubject();
            p = t.getPredicate();
            o = t.getObject();
            s_iri = ParsingUtils.parseNodeIRI(s);
            p_iri = ParsingUtils.parseNodeIRI(p);
            o_iri = ParsingUtils.parseNodeIRI(o);
            s_keyword = ParsingUtils.parseKeyword(s);
            p_keyword = ParsingUtils.parseKeyword(p);
            o_keyword = ParsingUtils.parseKeyword(o);
            encodeNode(k1, k2, k3, s_iri, VariablesPattern.S, p_keyword);
            encodeNode(k1, k2, k3, s_iri, VariablesPattern.S, o_keyword);
            encodeNode(k1, k2, k3, p_iri, VariablesPattern.P, s_keyword);
            encodeNode(k1, k2, k3, p_iri, VariablesPattern.P, o_keyword);
            encodeNode(k1, k2, k3, o_iri, VariablesPattern.O, s_keyword);
            encodeNode(k1, k2, k3, o_iri, VariablesPattern.O, p_keyword);
            encodeNode(k1, k2, k3, s_iri, VariablesPattern.S, String.format(COMPOUND_KEYWORD, p_keyword, o_keyword));
            encodeNode(k1, k2, k3, p_iri, VariablesPattern.P, String.format(COMPOUND_KEYWORD, s_keyword, o_keyword));
            encodeNode(k1, k2, k3, o_iri, VariablesPattern.O, String.format(COMPOUND_KEYWORD, s_keyword, p_keyword));
        }
    }

    private void encodeNode(SecretKey k1, SecretKey k2, SecretKey k3, String node, VariablesPattern pattern, String keyword)
            throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException,
            NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        keyword = String.format(KEYWORD_FORMAT, pattern, keyword);
        if (keyword.equals("S:S<:>http://www.w3.org/1999/02/22-rdf-syntax-ns#type:S<:>http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#Department"))
            System.out.println(node);
        byte[] st = generateDETLayer(k1, keyword.getBytes(StandardCharsets.UTF_8), getKeywordIV(keyword));
        byte[] ct = generateRNDLayer(k2, generateDETLayer(k3, node.getBytes(StandardCharsets.UTF_8), iv));
        encryptedT.put(
                base64Encoder.encodeToString(st),
                base64Encoder.encodeToString(ct)
        );
    }

    private void encryptKeywordInfo() throws InvalidAlgorithmParameterException,
            NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException,
            BadPaddingException, InvalidKeyException {
        for (Map.Entry<String, Pair<Integer, byte[]>> entry : keywords.entrySet()) {
            Pair<Integer, byte[]> value = entry.getValue();
            byte[] st = generateDETLayer(k1, entry.getKey().getBytes(StandardCharsets.UTF_8), iv);
            byte[] ct = generateRNDLayer(k2, Utils.integerToByteArray(value.getLeft()));
            encryptedT.put(
                    base64Encoder.encodeToString(st),
                    base64Encoder.encodeToString(ct)
            );
        }
    }

    public Map<String, String> generateKeywordTrapdoorMap(List<Triple> triples) throws InvalidNodeException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        HashMap<String, String> res = new HashMap<>();
        Set<String> skip = new HashSet<>();
        String s, p, o, po, so, sp;
        for (Triple t : triples) {
            s = ParsingUtils.parseKeyword(t.getSubject());
            p = ParsingUtils.parseKeyword(t.getPredicate());
            o = ParsingUtils.parseKeyword(t.getObject());
            po = String.format(COMPOUND_KEYWORD, p, o);
            so = String.format(COMPOUND_KEYWORD, s, o);
            sp = String.format(COMPOUND_KEYWORD, s, p);
            generateKeywordTrapdoor(res, skip, VariablesPattern.P, s);
            generateKeywordTrapdoor(res, skip, VariablesPattern.O, s);
            generateKeywordTrapdoor(res, skip, VariablesPattern.S, p);
            generateKeywordTrapdoor(res, skip, VariablesPattern.O, p);
            generateKeywordTrapdoor(res, skip, VariablesPattern.S, o);
            generateKeywordTrapdoor(res, skip, VariablesPattern.P, o);
            generateKeywordTrapdoor(res, skip, VariablesPattern.S, po);
            generateKeywordTrapdoor(res, skip, VariablesPattern.P, so);
            generateKeywordTrapdoor(res, skip, VariablesPattern.O, sp);
        }
        return res;
    }

    public List<String> generateTrapdoors(String keyword, int total)
            throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException,
            NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        byte[] st;
        List<String> trapdoors = new ArrayList<>(total);
        for (int i = 1; i < total; i++) {
            st = generateDETLayer(k1, keyword.getBytes(StandardCharsets.UTF_8), getKeywordIV(keyword));
            trapdoors.add(base64Encoder.encodeToString(st));
        }
        return trapdoors;
    }

    public byte[] generateRNDLayer(SecretKey key, byte[] deterministicCiphertext) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return SymmetricCipher.encrypt(deterministicCiphertext, key);
    }

    public byte[] generateDETLayer(SecretKey key, byte[] plaintext, byte[] iv) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return SymmetricCipher.encrypt(plaintext, key, iv);
    }

    public byte[] encryptDET(byte[] plaintext) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return generateDETLayer(k3, plaintext, iv);
    }

    private byte[] getKeywordIV(String keyword) {
        Pair<Integer, byte[]> entry = keywords.get(keyword);
        if (entry == null) {
            entry = new Pair<>(1, SymmetricCipher.incrementIV(iv));
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
        Map<String, Pair<Integer, byte[]>> res = new HashMap<>();
        int i = 0;
        for (String encTotal : keywordsTotals) {
            total = Utils.integerFromByteArray(SymmetricCipher.decrypt(k2, base64Decoder.decode(encTotal)));
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

    public String generateTrapdoor(String keyword) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        return base64Encoder.encodeToString(generateDETLayer(k1, keyword.getBytes(StandardCharsets.UTF_8), iv));
    }

    public byte[] decryptRNDLayer(String ciphertext) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        return SymmetricCipher.decrypt(k2, base64Decoder.decode(ciphertext));
    }

    public void updateKeywords(Map<String, Pair<Integer, byte[]>> values) {
        keywords.putAll(values);
    }


}
