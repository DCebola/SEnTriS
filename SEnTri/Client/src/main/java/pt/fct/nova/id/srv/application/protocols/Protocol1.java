package pt.fct.nova.id.srv.application.protocols;

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

import static pt.fct.nova.id.srv.application.query.jobs.VariablesPattern.*;

public class Protocol1 implements EncryptionProtocol {
    private final byte[] ivDET;
    private final byte[] ivRND;
    private final SecretKey kMASTER, kRND, kDET;
    private final Map<String, String> encryptedNodes;
    private final Map<String, Integer> keywordFrequency;
    private final Map<String, byte[]> keywordsIVs;
    private final Map<String, SecretKey> keywordDerivedKeys;
    private final Base64.Decoder base64Decoder;
    private final Base64.Encoder base64Encoder;

    public Protocol1(SecretKey kMASTER, SecretKey kRND, SecretKey kDET, byte[] iv) {
        this.ivDET = iv;
        this.ivRND = SymmetricCipher.generateZeroFilledIV();
        this.kMASTER = kMASTER;
        this.kRND = kRND;
        this.kDET = kDET;
        this.encryptedNodes = new HashMap<>();
        this.keywordFrequency = new HashMap<>();
        this.keywordDerivedKeys = new HashMap<>();
        this.keywordsIVs = new HashMap<>();
        this.base64Decoder = Base64.getUrlDecoder();
        this.base64Encoder = Base64.getUrlEncoder();
    }

    public Protocol1() throws NoSuchAlgorithmException {
        this.ivDET = SymmetricCipher.generateRandomIV();
        this.ivRND = SymmetricCipher.generateZeroFilledIV();
        this.kMASTER = SymmetricCipher.generateKey();
        this.kRND = SymmetricCipher.generateKey();
        this.kDET = SymmetricCipher.generateKey();
        this.encryptedNodes = new HashMap<>();
        this.keywordFrequency = new HashMap<>();
        this.keywordDerivedKeys = new HashMap<>();
        this.keywordsIVs = new HashMap<>();
        this.base64Decoder = Base64.getUrlDecoder();
        this.base64Encoder = Base64.getUrlEncoder();
    }

    public byte[] getIvDET() {
        return ivDET;
    }

    public SecretKey getKeywordsMasterKey() {
        return kMASTER;
    }

    public SecretKey getRNDKey() {
        return kRND;
    }

    public SecretKey getDETKey() {
        return kDET;
    }

    public Map<String, Integer> getKeywordFrequency() {
        return keywordFrequency;
    }

    public Map<String, String> getEncryptedNodes() {
        return encryptedNodes;
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
            encodeNode(p_iri, PO, s_keyword);
            encodeNode(o_iri, PO, s_keyword);
            encodeNode(s_iri, SO, p_keyword);
            encodeNode(o_iri, SO, p_keyword);
            encodeNode(s_iri, SP, o_keyword);
            encodeNode(p_iri, SP, o_keyword);
            encodeNode(s_iri, S, String.format(COMPOUND_KEYWORD, p_keyword, o_keyword));
            encodeNode(p_iri, P, String.format(COMPOUND_KEYWORD, s_keyword, o_keyword));
            encodeNode(o_iri, O, String.format(COMPOUND_KEYWORD, s_keyword, p_keyword));
        }
    }

    private void encodeNode(String node, VariablesPattern pattern, String keyword)
            throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException,
            NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        keyword = ParsingUtils.generateKeyword(pattern, keyword);
        byte[] st = generateDETLayer(getKeywordDerivedKey(keyword), keyword.getBytes(StandardCharsets.UTF_8), getKeywordIV(keyword));
        byte[] ct = generateRNDLayer(generateDETLayer(kDET, node.getBytes(StandardCharsets.UTF_8), ivDET));
        encryptedNodes.put(
                base64Encoder.encodeToString(st),
                base64Encoder.encodeToString(ct)
        );
        incrementKeywordFrequency(keyword);
    }

    private void encryptKeywordInfo() throws InvalidAlgorithmParameterException,
            NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException,
            BadPaddingException, InvalidKeyException {
        for (String keyword : keywordFrequency.keySet()) {
            byte[] st = generateDETLayer(keywordDerivedKeys.get(keyword), keyword.getBytes(StandardCharsets.UTF_8), SymmetricCipher.generateZeroFilledIV());
            byte[] ct = generateRNDLayer(ProtocolUtils.integerToByteArray(keywordFrequency.get(keyword)));
            encryptedNodes.put(
                    base64Encoder.encodeToString(st),
                    base64Encoder.encodeToString(ct)
            );
        }
    }

    public Map<String, Set<String>> generateKeywordsAndEncryptedValues(List<Triple> triples) throws InvalidNodeException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        Map<String, Set<String>> keywordsAndEncryptedNodes = new HashMap<>();
        Node s, p, o;
        String s_iri, p_iri, o_iri, s_keyword, p_keyword, o_keyword;
        for (Triple t : triples) {
            s = t.getSubject();
            p = t.getPredicate();
            o = t.getObject();
            s_iri = base64Encoder.encodeToString(generateDETLayer(kDET, ParsingUtils.parseNodeIRI(s).getBytes(StandardCharsets.UTF_8), ivDET));
            p_iri = base64Encoder.encodeToString(generateDETLayer(kDET, ParsingUtils.parseNodeIRI(p).getBytes(StandardCharsets.UTF_8), ivDET));
            o_iri = base64Encoder.encodeToString(generateDETLayer(kDET, ParsingUtils.parseNodeIRI(o).getBytes(StandardCharsets.UTF_8), ivDET));
            s_keyword = ParsingUtils.parseKeyword(s);
            p_keyword = ParsingUtils.parseKeyword(p);
            o_keyword = ParsingUtils.parseKeyword(o);
            addEncryptedNode(keywordsAndEncryptedNodes, PO, s_keyword, p_iri);
            addEncryptedNode(keywordsAndEncryptedNodes, PO, s_keyword, o_iri);
            addEncryptedNode(keywordsAndEncryptedNodes, SO, p_keyword, s_iri);
            addEncryptedNode(keywordsAndEncryptedNodes, SO, p_keyword, o_iri);
            addEncryptedNode(keywordsAndEncryptedNodes, SP, p_keyword, s_iri);
            addEncryptedNode(keywordsAndEncryptedNodes, SP, p_keyword, p_iri);
            addEncryptedNode(keywordsAndEncryptedNodes, S, String.format(COMPOUND_KEYWORD, p_keyword, o_keyword), s_iri);
            addEncryptedNode(keywordsAndEncryptedNodes, P, String.format(COMPOUND_KEYWORD, s_keyword, o_keyword), p_iri);
            addEncryptedNode(keywordsAndEncryptedNodes, O, String.format(COMPOUND_KEYWORD, s_keyword, p_keyword), o_iri);
        }
        return keywordsAndEncryptedNodes;
    }

    private void addEncryptedNode(Map<String, Set<String>> collector, VariablesPattern pattern, String keyword, String encryptedNode) {
        keyword = ParsingUtils.generateKeyword(pattern, keyword);
        Set<String> encryptedValues = collector.get(keyword);
        if (encryptedValues == null) {
            encryptedValues = new HashSet<>();
            encryptedValues.add(encryptedNode);
            collector.put(keyword, encryptedValues);
        } else
            encryptedValues.add(encryptedNode);
    }

    public byte[] generateRNDLayer(byte[] deterministicCiphertext) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return SymmetricCipher.encrypt(deterministicCiphertext, kRND, SymmetricCipher.generateRandomIV());
    }

    public byte[] generateDETLayer(SecretKey key, byte[] plaintext, byte[] iv) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return SymmetricCipher.encrypt(plaintext, key, iv);
    }

    public String generateKeywordsFrequencyTrapdoor(String keyword) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        return base64Encoder.encodeToString(generateDETLayer(getKeywordDerivedKey(keyword), keyword.getBytes(StandardCharsets.UTF_8), SymmetricCipher.generateZeroFilledIV()));
    }

    public String generateTrapdoor(String keyword) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        return base64Encoder.encodeToString(generateDETLayer(getKeywordDerivedKey(keyword), keyword.getBytes(StandardCharsets.UTF_8), getKeywordIV(keyword)));
    }

    public byte[] decryptRNDLayer(String ciphertext) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        return SymmetricCipher.decrypt(kRND, base64Decoder.decode(ciphertext));
    }

    public byte[] decryptDETLayer(String ciphertext) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        return SymmetricCipher.decrypt(kDET, base64Decoder.decode(ciphertext));
    }

    private SecretKey getKeywordDerivedKey(String keyword) {
        SecretKey keywordDerivedKey = keywordDerivedKeys.get(keyword);
        if (keywordDerivedKey == null) {
            keywordDerivedKey = SymmetricCipher.generateKey(kMASTER, keyword.getBytes(StandardCharsets.UTF_8));
            keywordDerivedKeys.put(keyword, keywordDerivedKey);
        }
        return keywordDerivedKey;
    }

    private byte[] getKeywordIV(String keyword) {
        byte[] keywordIV = keywordsIVs.get(keyword);
        if (keywordIV == null) {
            keywordIV = SymmetricCipher.generateZeroFilledIV();
            SymmetricCipher.incrementIV(keywordIV);
            keywordsIVs.put(keyword, keywordIV);
        } else
            SymmetricCipher.incrementIV(keywordIV);
        return keywordIV;
    }

    private void incrementKeywordFrequency(String keyword) {
        Integer keywordCount = keywordFrequency.get(keyword);
        if (keywordCount == null) {
            keywordCount = 1;
            keywordFrequency.put(keyword, keywordCount);
        } else
            keywordFrequency.put(keyword, keywordCount + 1);
    }

    private void subtractKeywordFrequency(String keyword) {
        Integer keywordCount = keywordFrequency.get(keyword);
        if (keywordCount != null && keywordCount > 0)
            keywordFrequency.put(keyword, keywordCount - 1);
    }

    public void setKeywordsFrequencies(Map<String, Integer> newKeywordsFrequencies) {
        int max = -1;
        int frequency;
        byte[] iv = SymmetricCipher.generateZeroFilledIV();
        Map<Integer, byte[]> generatedIvs = new HashMap<>();
        generatedIvs.put(0, iv);
        for (String keyword : newKeywordsFrequencies.keySet()) {
            frequency = newKeywordsFrequencies.get(keyword);
            if (frequency > max) {
                for (int j = 0; j < frequency - max; j++) {
                    SymmetricCipher.incrementIV(iv);
                    generatedIvs.put(frequency - j, iv.clone());
                }
                max = frequency;
            }
            keywordsIVs.put(keyword, generatedIvs.get(frequency).clone());
            keywordFrequency.put(keyword, frequency);
        }
    }

    public void deleteKeyword(String keyword) {
        Integer keywordCount = keywordFrequency.get(keyword);
        if (keywordCount != null && keywordCount > 0)
            keywordFrequency.put(keyword, keywordCount - 1);
        byte[] keywordIV = keywordsIVs.get(keyword);
        if (keywordIV != null) {
            keywordIV = SymmetricCipher.generateZeroFilledIV();
            SymmetricCipher.decrementIV(keywordIV);
            keywordsIVs.put(keyword, keywordIV);
        }
    }
}
