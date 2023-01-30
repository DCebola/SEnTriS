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

import static pt.fct.nova.id.srv.application.query.QueryUtils.generateID;
import static pt.fct.nova.id.srv.application.query.jobs.VariablesPattern.*;

public class Protocol1 implements EncryptionProtocol {
    private final byte[] ivDET;
    private final SecretKey kMASTER, kRND, kDET;
    private final Map<String, String> encryptedNodes;
    private final Map<String, Integer> keywordFrequencies;
    private final Map<String, SecretKey> keywordDerivedKeys;
    private final Base64.Decoder base64Decoder;
    private final Base64.Encoder base64Encoder;
    private final String schemaKeyword;

    public Protocol1(SecretKey kMASTER, SecretKey kRND, SecretKey kDET, byte[] iv, String schemaKeyword) {
        this.ivDET = iv;
        this.kMASTER = kMASTER;
        this.kRND = kRND;
        this.kDET = kDET;
        this.schemaKeyword = schemaKeyword;
        this.encryptedNodes = new HashMap<>();
        this.keywordFrequencies = new HashMap<>();
        this.keywordDerivedKeys = new HashMap<>();
        this.base64Decoder = Base64.getUrlDecoder();
        this.base64Encoder = Base64.getUrlEncoder();
    }

    public Protocol1() throws NoSuchAlgorithmException {
        this.ivDET = SymmetricCipher.generateRandomIV();
        this.kMASTER = SymmetricCipher.generateKey();
        this.kRND = SymmetricCipher.generateKey();
        this.kDET = SymmetricCipher.generateKey();
        this.schemaKeyword = generateID();
        this.encryptedNodes = new HashMap<>();
        this.keywordFrequencies = new HashMap<>();
        this.keywordDerivedKeys = new HashMap<>();
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

    public Map<String, Integer> getKeywordFrequencies() {
        return keywordFrequencies;
    }

    @Override
    public void exec(List<Triple> triples, boolean schema) throws InvalidNodeException, InvalidAlgorithmParameterException,
            NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException,
            InvalidKeyException, RuntimeException {
        if (schema)
            encryptSchemaTriples(triples);
        else
            encryptTriples(triples);
        encryptKeywordInfo();
    }

    private void encryptSchemaTriples(List<Triple> triples) throws InvalidNodeException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        for (Triple t : triples) {
            encodeSchemaNode(ParsingUtils.parseNode(t.getSubject()));
            encodeSchemaNode(ParsingUtils.parseNode(t.getPredicate()));
            encodeSchemaNode(ParsingUtils.parseNode(t.getObject()));
        }
    }

    private int encodeSchemaNode(String node) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        int frequency = incrementKeywordFrequency(schemaKeyword);
        byte[] st = generateDETLayer(getKeywordDerivedKey(schemaKeyword), schemaKeyword.getBytes(StandardCharsets.UTF_8), SymmetricCipher.ivFromInteger(frequency));
        byte[] ct = generateRNDLayer(node.getBytes(StandardCharsets.UTF_8));
        encryptedNodes.put(base64Encoder.encodeToString(st), base64Encoder.encodeToString(ct));
        return frequency;
    }


    private void encryptTriples(List<Triple> triples) throws InvalidNodeException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        Node s, p, o;
        String parsed_s, parsed_p, parsed_o, s_keyword, p_keyword, o_keyword, t_keyword;
        List<Integer> frequencies;
        Set<String> processed = new HashSet<>();
        for (Triple t : triples) {
            frequencies = new ArrayList<>(9);
            s = t.getSubject();
            p = t.getPredicate();
            o = t.getObject();
            parsed_s = ParsingUtils.parseNode(s);
            parsed_p = ParsingUtils.parseNode(p);
            parsed_o = ParsingUtils.parseNode(o);
            s_keyword = ParsingUtils.parseKeyword(s);
            p_keyword = ParsingUtils.parseKeyword(p);
            o_keyword = ParsingUtils.parseKeyword(o);
            t_keyword = String.format(TRIPLE_KEYWORD, s_keyword, p_keyword, o_keyword);
            if (!processed.contains(t_keyword)) {
                processed.add(t_keyword);
                frequencies.add(encodeNode(parsed_p, PO, s_keyword));
                frequencies.add(encodeNode(parsed_o, PO, s_keyword));
                frequencies.add(encodeNode(parsed_s, SO, p_keyword));
                frequencies.add(encodeNode(parsed_o, SO, p_keyword));
                frequencies.add(encodeNode(parsed_s, SP, o_keyword));
                frequencies.add(encodeNode(parsed_p, SP, o_keyword));
                frequencies.add(encodeNode(parsed_s, S, String.format(COMPOUND_KEYWORD, p_keyword, o_keyword)));
                frequencies.add(encodeNode(parsed_p, P, String.format(COMPOUND_KEYWORD, s_keyword, o_keyword)));
                frequencies.add(encodeNode(parsed_o, O, String.format(COMPOUND_KEYWORD, s_keyword, p_keyword)));
                encodeTriple(t_keyword, frequencies);
            }
        }
    }

    private int encodeNode(String node, VariablesPattern pattern, String keyword)
            throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException,
            NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        keyword = ParsingUtils.generateKeyword(pattern, keyword);
        int frequency = incrementKeywordFrequency(keyword);
        byte[] st = generateDETLayer(getKeywordDerivedKey(keyword), keyword.getBytes(StandardCharsets.UTF_8), SymmetricCipher.ivFromInteger(frequency));
        byte[] ct = generateRNDLayer(generateDETLayer(kDET, node.getBytes(StandardCharsets.UTF_8), ivDET));
        encryptedNodes.put(base64Encoder.encodeToString(st), base64Encoder.encodeToString(ct));
        return frequency;
    }

    private void encodeTriple(String keyword, List<Integer> frequencies) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException,
            NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        keyword = ParsingUtils.generateKeyword(SPO, keyword);
        byte[] st;
        int i = 0;
        for (int f : frequencies) {
            st = generateDETLayer(getKeywordDerivedKey(keyword), keyword.getBytes(StandardCharsets.UTF_8), SymmetricCipher.ivFromInteger(i));
            encryptedNodes.put(base64Encoder.encodeToString(st), base64Encoder.encodeToString(generateRNDLayer(ParsingUtils.integerToByteArray(f))));
            i++;
        }
    }

    public Map<String, List<String>> generateKeywordsPatternTrapdoors(List<Triple> triples) throws InvalidNodeException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        Map<String, List<String>> res = new HashMap<>(triples.size() * 6);
        String s, p, o, t_keyword;
        List<String> keywords;
        Set<String> processed = new HashSet<>();
        for (Triple t : triples) {
            keywords = new ArrayList<>(9);
            s = ParsingUtils.parseKeyword(t.getSubject());
            p = ParsingUtils.parseKeyword(t.getPredicate());
            o = ParsingUtils.parseKeyword(t.getObject());
            t_keyword = String.format(TRIPLE_KEYWORD, s, p, o);
            if (!processed.contains(t_keyword)) {
                processed.add(t_keyword);
                keywords.add(ParsingUtils.generateKeyword(PO, s));
                keywords.add(ParsingUtils.generateKeyword(PO, s));
                keywords.add(ParsingUtils.generateKeyword(SO, p));
                keywords.add(ParsingUtils.generateKeyword(SO, p));
                keywords.add(ParsingUtils.generateKeyword(SP, o));
                keywords.add(ParsingUtils.generateKeyword(SP, o));
                keywords.add(ParsingUtils.generateKeyword(S, p, o));
                keywords.add(ParsingUtils.generateKeyword(P, s, o));
                keywords.add(ParsingUtils.generateKeyword(O, s, p));
                generatePatternTrapdoors(res, t_keyword, keywords);
            }
        }
        System.out.println("KeywordsPatternTrapdoors: " + res.size());
        return res;
    }

    private void encryptKeywordInfo() throws InvalidAlgorithmParameterException,
            NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException,
            BadPaddingException, InvalidKeyException {
        for (String keyword : keywordFrequencies.keySet()) {
            byte[] st = generateDETLayer(keywordDerivedKeys.get(keyword), keyword.getBytes(StandardCharsets.UTF_8), SymmetricCipher.generateZeroFilledIV());
            byte[] ct = generateRNDLayer(ParsingUtils.integerToByteArray(keywordFrequencies.get(keyword)));
            encryptedNodes.put(
                    base64Encoder.encodeToString(st),
                    base64Encoder.encodeToString(ct)
            );
        }
    }

    private void generatePatternTrapdoors(Map<String, List<String>> keywordPatternTrapdoors, String tripleKeyword, List<String> keywords) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        tripleKeyword = ParsingUtils.generateKeyword(SPO, tripleKeyword);
        List<String> trapdoors;
        int i = 0;
        for (String keyword : keywords) {
            trapdoors = keywordPatternTrapdoors.get(keyword);
            if (trapdoors == null)
                trapdoors = new LinkedList<>();
            trapdoors.add(base64Encoder.encodeToString(generateDETLayer(getKeywordDerivedKey(tripleKeyword),
                    tripleKeyword.getBytes(StandardCharsets.UTF_8), SymmetricCipher.ivFromInteger(i))));
            keywordPatternTrapdoors.put(keyword, trapdoors);
            i++;
        }
    }

    public byte[] generateRNDLayer(byte[] deterministicCiphertext) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return SymmetricCipher.encrypt(deterministicCiphertext, kRND);
    }

    public byte[] generateDETLayer(SecretKey key, byte[] plaintext, byte[] iv) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return SymmetricCipher.encrypt(plaintext, key, iv);
    }

    public String generateKeywordsFrequencyTrapdoor(String keyword) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        return base64Encoder.encodeToString(generateDETLayer(getKeywordDerivedKey(keyword), keyword.getBytes(StandardCharsets.UTF_8), SymmetricCipher.generateZeroFilledIV()));
    }

    public String generateTrapdoorAndIncrementIV(String keyword) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        return base64Encoder.encodeToString(generateDETLayer(getKeywordDerivedKey(keyword),
                keyword.getBytes(StandardCharsets.UTF_8), SymmetricCipher.ivFromInteger(incrementKeywordFrequency(keyword))));
    }

    private int incrementKeywordFrequency(String keyword) {
        return keywordFrequencies.merge(keyword, 1, Integer::sum);
    }

    public String generateTrapdoor(String keyword, int value) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        return base64Encoder.encodeToString(generateDETLayer(getKeywordDerivedKey(keyword), keyword.getBytes(StandardCharsets.UTF_8), SymmetricCipher.ivFromInteger(value)));
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

    public void setKeywordFrequencies(Map<String, Integer> values) {
        keywordFrequencies.clear();
        int frequency;
        for (String keyword : values.keySet()) {
            frequency = values.get(keyword);
            if (frequency > 0)
                keywordFrequencies.put(keyword, frequency);
        }
    }

    public void deleteKeyword(String keyword) {
        Integer frequency = keywordFrequencies.get(keyword);
        if (frequency != null) {
            if (frequency <= 1)
                keywordFrequencies.remove(keyword);
            else
                keywordFrequencies.put(keyword, frequency - 1);
        }
    }


    public String getSchemaKeyword() {
        return schemaKeyword;
    }

}
