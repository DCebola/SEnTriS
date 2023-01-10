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
    private final SecretKey kMASTER, kRND, kDET;
    private final Map<String, String> encryptedNodes;
    private final Map<String, Integer> keywordFrequency;
    private final Map<String, byte[]> keywordsIVs;
    private final Map<String, SecretKey> keywordDerivedKeys;
    private final Base64.Decoder base64Decoder;
    private final Base64.Encoder base64Encoder;

    public Protocol1(SecretKey kMASTER, SecretKey kRND, SecretKey kDET, byte[] iv) {
        this.ivDET = iv;
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
        List<byte[]> ivs;
        for (Triple t : triples) {
            ivs = new ArrayList<>(9);
            s = t.getSubject();
            p = t.getPredicate();
            o = t.getObject();
            s_iri = ParsingUtils.parseNodeIRI(s);
            p_iri = ParsingUtils.parseNodeIRI(p);
            o_iri = ParsingUtils.parseNodeIRI(o);
            s_keyword = ParsingUtils.parseKeyword(s);
            p_keyword = ParsingUtils.parseKeyword(p);
            o_keyword = ParsingUtils.parseKeyword(o);
            ivs.add(encodeNode(p_iri, PO, s_keyword));
            ivs.add(encodeNode(o_iri, PO, s_keyword));
            ivs.add(encodeNode(s_iri, SO, p_keyword));
            ivs.add(encodeNode(o_iri, SO, p_keyword));
            ivs.add(encodeNode(s_iri, SP, o_keyword));
            ivs.add(encodeNode(p_iri, SP, o_keyword));
            ivs.add(encodeNode(s_iri, S, String.format(COMPOUND_KEYWORD, p_keyword, o_keyword)));
            ivs.add(encodeNode(p_iri, P, String.format(COMPOUND_KEYWORD, s_keyword, o_keyword)));
            ivs.add(encodeNode(o_iri, O, String.format(COMPOUND_KEYWORD, s_keyword, p_keyword)));
            encodeTriple(String.format(TRIPLE_KEYWORD, s_keyword, p_keyword, o_keyword), ivs);
        }
    }

    private byte[] encodeNode(String node, VariablesPattern pattern, String keyword)
            throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException,
            NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        keyword = ParsingUtils.generateKeyword(pattern, keyword);
        byte[] iv = getKeywordIV(keyword);
        byte[] st = generateDETLayer(getKeywordDerivedKey(keyword), keyword.getBytes(StandardCharsets.UTF_8), iv);
        byte[] ct = generateRNDLayer(generateDETLayer(kDET, node.getBytes(StandardCharsets.UTF_8), ivDET));
        encryptedNodes.put(base64Encoder.encodeToString(st), base64Encoder.encodeToString(ct));
        incrementKeywordFrequency(keyword);
        return iv;
    }

    private void encodeTriple(String keyword, List<byte[]> ivs)
            throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException,
            NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        keyword = ParsingUtils.generateKeyword(SPO, keyword);
        byte[] keywordIV = SymmetricCipher.generateZeroFilledIV();
        byte[] st;
        for (byte[] iv : ivs) {
            st = generateDETLayer(getKeywordDerivedKey(keyword), keyword.getBytes(StandardCharsets.UTF_8), keywordIV);
            encryptedNodes.put(base64Encoder.encodeToString(st), base64Encoder.encodeToString(generateRNDLayer(iv)));
            SymmetricCipher.incrementIV(keywordIV);
        }
    }


    public Map<String, List<String>> generateKeywordsPatternTrapdoors(List<Triple> triples) throws InvalidNodeException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        Map<String, List<String>> res = new HashMap<>(triples.size() * 6);
        String s, p, o;
        List<String> keywords;
        for (Triple t : triples) {
            keywords = new ArrayList<>(9);
            s = ParsingUtils.parseKeyword(t.getSubject());
            p = ParsingUtils.parseKeyword(t.getPredicate());
            o = ParsingUtils.parseKeyword(t.getObject());
            keywords.add(ParsingUtils.generateKeyword(PO, s));
            keywords.add(ParsingUtils.generateKeyword(PO, s));
            keywords.add(ParsingUtils.generateKeyword(SO, p));
            keywords.add(ParsingUtils.generateKeyword(SO, p));
            keywords.add(ParsingUtils.generateKeyword(SP, o));
            keywords.add(ParsingUtils.generateKeyword(SP, o));
            keywords.add(ParsingUtils.generateKeyword(S, p, o));
            keywords.add(ParsingUtils.generateKeyword(P, s, o));
            keywords.add(ParsingUtils.generateKeyword(O, s, p));
            generatePatternTrapdoors(res, String.format(TRIPLE_KEYWORD, s, p, o), keywords);
        }
        return res;
    }

    private void generatePatternTrapdoors(Map<String, List<String>> keywordPatternTrapdoors, String tripleKeyword, List<String> keywords) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        tripleKeyword = ParsingUtils.generateKeyword(SPO, tripleKeyword);
        byte[] iv = SymmetricCipher.generateZeroFilledIV();
        String trapdoor;
        List<String> trapdoors;
        for (String keyword : keywords) {
            trapdoor = base64Encoder.encodeToString(generateDETLayer(getKeywordDerivedKey(tripleKeyword), tripleKeyword.getBytes(StandardCharsets.UTF_8), iv));
            trapdoors = keywordPatternTrapdoors.get(keyword);
            if (trapdoors == null) {
                trapdoors = new LinkedList<>();
                trapdoors.add(trapdoor);
            } else
                trapdoors.add(trapdoor);
            SymmetricCipher.incrementIV(iv);
        }
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

    public List<String> generateTrapdoors(String keyword, List<Integer> instances) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        List<String> trapdoors = new ArrayList<>(instances.size());
        int current = 0;
        byte[] iv = SymmetricCipher.generateZeroFilledIV();
        for (int i : instances) {
            if (i == current)
                trapdoors.add(base64Encoder.encodeToString(generateDETLayer(getKeywordDerivedKey(keyword), keyword.getBytes(StandardCharsets.UTF_8), iv)));
            else
                SymmetricCipher.incrementIV(iv);
        }
        return trapdoors;
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
