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

public class Protocol1 implements EncryptionProtocol {
    private final byte[] ivDET;
    private final byte[] ivRND;
    private final SecretKey kMASTER, kRND, kDET;
    private final Map<String, String> encryptedT;
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
        this.encryptedT = new HashMap<>();
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
        this.encryptedT = new HashMap<>();
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
            encodeNode(s_iri, VariablesPattern.S, o_keyword);
            encodeNode(s_iri, VariablesPattern.S, p_keyword);
            encodeNode(p_iri, VariablesPattern.P, s_keyword);
            encodeNode(p_iri, VariablesPattern.PO, s_keyword);
            encodeNode(o_iri, VariablesPattern.PO, s_keyword);
            encodeNode(s_iri, VariablesPattern.SO, p_keyword);
            encodeNode(o_iri, VariablesPattern.SO, p_keyword);
            encodeNode(s_iri, VariablesPattern.SP, o_keyword);
            encodeNode(p_iri, VariablesPattern.SP, o_keyword);
            encodeNode(s_iri, VariablesPattern.S, String.format(COMPOUND_KEYWORD, p_keyword, o_keyword));
            encodeNode(p_iri, VariablesPattern.P, String.format(COMPOUND_KEYWORD, s_keyword, o_keyword));
            encodeNode(o_iri, VariablesPattern.O, String.format(COMPOUND_KEYWORD, s_keyword, p_keyword));
        }
    }

    private void encodeNode(String node, VariablesPattern pattern, String keyword)
            throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException,
            NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        keyword = String.format(KEYWORD_FORMAT, pattern, keyword);
        byte[] st = generateDETLayer(getKeywordDerivedKey(keyword), keyword.getBytes(StandardCharsets.UTF_8), getKeywordIV(keyword));
        byte[] ct = generateRNDLayer(generateDETLayer(kDET, node.getBytes(StandardCharsets.UTF_8), ivDET));
        encryptedT.put(
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
            byte[] ct = generateRNDLayer(Utils.integerToByteArray(keywordFrequency.get(keyword)));
            encryptedT.put(
                    base64Encoder.encodeToString(st),
                    base64Encoder.encodeToString(ct)
            );
        }
    }

    public byte[] generateRNDLayer(byte[] deterministicCiphertext) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        byte[] ciphertext = SymmetricCipher.encrypt(deterministicCiphertext, kRND, ivRND);
        SymmetricCipher.incrementIV(ivRND);
        return ciphertext;
    }

    public byte[] generateDETLayer(SecretKey key, byte[] plaintext, byte[] iv) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return SymmetricCipher.encrypt(plaintext, key, iv);
    }

    public byte[] encryptDET(byte[] plaintext) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return generateDETLayer(kDET, plaintext, ivDET);
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

    public Map<String, String> init(List<Triple> triples) throws InvalidNodeException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        HashMap<String, String> trapdoors = new HashMap<>();
        String s, p, o, po, so, sp;
        for (Triple t : triples) {
            s = ParsingUtils.parseKeyword(t.getSubject());
            p = ParsingUtils.parseKeyword(t.getPredicate());
            o = ParsingUtils.parseKeyword(t.getObject());
            po = String.format(COMPOUND_KEYWORD, p, o);
            so = String.format(COMPOUND_KEYWORD, s, o);
            sp = String.format(COMPOUND_KEYWORD, s, p);
            generateDistinctKeywordTrapdoor(trapdoors, VariablesPattern.P, s);
            generateDistinctKeywordTrapdoor(trapdoors, VariablesPattern.O, s);
            generateDistinctKeywordTrapdoor(trapdoors, VariablesPattern.S, p);
            generateDistinctKeywordTrapdoor(trapdoors, VariablesPattern.O, p);
            generateDistinctKeywordTrapdoor(trapdoors, VariablesPattern.S, o);
            generateDistinctKeywordTrapdoor(trapdoors, VariablesPattern.P, o);
            generateDistinctKeywordTrapdoor(trapdoors, VariablesPattern.S, po);
            generateDistinctKeywordTrapdoor(trapdoors, VariablesPattern.P, so);
            generateDistinctKeywordTrapdoor(trapdoors, VariablesPattern.O, sp);
        }
        return trapdoors;
    }

    private void generateDistinctKeywordTrapdoor(HashMap<String, String> trapdoors, VariablesPattern pattern, String keyword) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        keyword = String.format(KEYWORD_FORMAT, pattern, keyword);
        if (!trapdoors.containsKey(keyword))
            trapdoors.put(keyword, generateTrapdoor(keyword));
    }

    public void update(List<String> keywords, List<String> keywordFrequency) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        int max = -1;
        int frequency;
        byte[] iv = SymmetricCipher.generateZeroFilledIV();
        Map<Integer, byte[]> generatedIvs = new HashMap<>();
        generatedIvs.put(0, iv);
        int i = 0;
        for (String f : keywordFrequency) {
            frequency = Utils.integerFromByteArray(decryptRNDLayer(f));
            if (frequency > max) {
                for (int j = 0; j < frequency - max; j++) {
                    SymmetricCipher.incrementIV(iv);
                    generatedIvs.put(frequency - j, iv.clone());
                }
                max = frequency;
            }
            keywordsIVs.put(keywords.get(i), generatedIvs.get(frequency).clone());
            this.keywordFrequency.put(keywords.get(i), frequency);
            i++;
        }
    }


}
