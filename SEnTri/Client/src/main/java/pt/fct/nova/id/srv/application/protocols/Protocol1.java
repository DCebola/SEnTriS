package pt.fct.nova.id.srv.application.protocols;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import pt.fct.nova.id.srv.application.crypto.SymmetricEncryptionUtils;
import pt.fct.nova.id.srv.application.protocols.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.presentation.controllers.ParsingUtils;

import javax.crypto.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static pt.fct.nova.id.srv.application.query.QueryUtils.generateID;
import static pt.fct.nova.id.srv.application.query.jobs.VariablesPattern.*;

public class Protocol1 implements EncryptionProtocol {
    private final byte[] ivDET;
    private final SecretKey kMASTER, kRND, kDET;
    private final Map<String, String> encryptedNodes;
    private final Map<String, Integer> keywordFrequencies;
    private final Map<String, SecretKey> derivedKeys;
    private final String schemaKeyword;
    private final byte[] frequencyIV;
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder();

    public Protocol1(SecretKey kMASTER, SecretKey kRND, SecretKey kDET, byte[] iv, String schemaKeyword) {
        this.ivDET = iv;
        this.kMASTER = kMASTER;
        this.kRND = kRND;
        this.kDET = kDET;
        this.schemaKeyword = schemaKeyword;
        this.encryptedNodes = new HashMap<>();
        this.keywordFrequencies = new HashMap<>();
        this.derivedKeys = new HashMap<>();
        this.frequencyIV = SymmetricEncryptionUtils.generateZeroFilledIV();

    }

    public Protocol1() {
        this.ivDET = SymmetricEncryptionUtils.generateRandomIV();
        this.kMASTER = SymmetricEncryptionUtils.generateKey();
        this.kRND = SymmetricEncryptionUtils.generateKey();
        this.kDET = SymmetricEncryptionUtils.generateKey();
        this.schemaKeyword = generateID();
        this.encryptedNodes = new HashMap<>();
        this.keywordFrequencies = new HashMap<>();
        this.derivedKeys = new HashMap<>();
        this.frequencyIV = SymmetricEncryptionUtils.generateZeroFilledIV();
    }

    public String getSchemaKeyword() {
        return schemaKeyword;
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
    public void exec(Set<Triple> triples, boolean schema) throws InvalidNodeException {
        if (schema)
            encryptSchemaTriples(triples);
        else
            encryptTriples(triples);
        encryptKeywordInfo();
    }

    private void encryptSchemaTriples(Set<Triple> triples) throws InvalidNodeException {
        for (Triple t : triples) {
            encodeSchemaNode(ParsingUtils.parseNode(t.getSubject()));
            encodeSchemaNode(ParsingUtils.parseNode(t.getPredicate()));
            encodeSchemaNode(ParsingUtils.parseNode(t.getObject()));
        }
    }

    private int encodeSchemaNode(String node) {
        int frequency = incrementKeywordFrequency(schemaKeyword);
        byte[] st = generateDETLayer(getDerivedKey(schemaKeyword), schemaKeyword.getBytes(StandardCharsets.UTF_8), SymmetricEncryptionUtils.ivFromInteger(frequency));
        byte[] ct = generateRNDLayer(node.getBytes(StandardCharsets.UTF_8));
        encryptedNodes.put(base64Encoder.encodeToString(st), base64Encoder.encodeToString(ct));
        return frequency;
    }


    private void encryptTriples(Set<Triple> triples) throws InvalidNodeException {
        Node s, p, o;
        String parsed_s, parsed_p, parsed_o;
        String s_keyword, p_keyword, o_keyword, t_keyword;
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
                frequencies.add(encodeNode(parsed_p, ParsingUtils.generateKeyword(PO, s_keyword)));
                frequencies.add(encodeNode(parsed_o, ParsingUtils.generateKeyword(PO, s_keyword)));
                frequencies.add(encodeNode(parsed_s, ParsingUtils.generateKeyword(SO, p_keyword)));
                frequencies.add(encodeNode(parsed_o, ParsingUtils.generateKeyword(SO, p_keyword)));
                frequencies.add(encodeNode(parsed_s, ParsingUtils.generateKeyword(SP, o_keyword)));
                frequencies.add(encodeNode(parsed_p, ParsingUtils.generateKeyword(SP, o_keyword)));
                frequencies.add(encodeNode(parsed_s, ParsingUtils.generateKeyword(S, String.format(COMPOUND_KEYWORD, p_keyword, o_keyword))));
                frequencies.add(encodeNode(parsed_p, ParsingUtils.generateKeyword(P, String.format(COMPOUND_KEYWORD, s_keyword, o_keyword))));
                frequencies.add(encodeNode(parsed_o, ParsingUtils.generateKeyword(O, String.format(COMPOUND_KEYWORD, s_keyword, p_keyword))));
                encodeTriple(ParsingUtils.generateKeyword(SPO, t_keyword), frequencies);
            }
        }
    }

    private int encodeNode(String node, String keyword) {
        int frequency = incrementKeywordFrequency(keyword);
        byte[] st = generateDETLayer(getDerivedKey(keyword), keyword.getBytes(StandardCharsets.UTF_8), SymmetricEncryptionUtils.ivFromInteger(frequency));
        byte[] ct = generateRNDLayer(generateDETLayer(kDET, node.getBytes(StandardCharsets.UTF_8), ivDET));
        encryptedNodes.put(base64Encoder.encodeToString(st), base64Encoder.encodeToString(ct));
        return frequency;
    }

    private void encodeTriple(String keyword, List<Integer> frequencies) {
        byte[] st;
        int i = 0;
        for (int f : frequencies) {
            st = generateDETLayer(getDerivedKey(keyword), keyword.getBytes(StandardCharsets.UTF_8), SymmetricEncryptionUtils.ivFromInteger(i));
            encryptedNodes.put(base64Encoder.encodeToString(st), base64Encoder.encodeToString(generateRNDLayer(ParsingUtils.integerToByteArray(f))));
            i++;
        }
    }

    public Map<String, List<String>> generateKeywordsPatternTrapdoors(Set<Triple> triples) throws InvalidNodeException {
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
                generatePatternTrapdoors(res, ParsingUtils.generateKeyword(SPO, t_keyword), keywords);
            }
        }
        System.out.println("KeywordsPatternTrapdoors: " + res.size());
        return res;
    }

    private void encryptKeywordInfo() {
        byte[] st, ct;
        for (String keyword : keywordFrequencies.keySet()) {
            st = generateDETLayer(derivedKeys.get(keyword), keyword.getBytes(StandardCharsets.UTF_8), frequencyIV);
            ct = generateRNDLayer(ParsingUtils.integerToByteArray(keywordFrequencies.get(keyword)));
            encryptedNodes.put(base64Encoder.encodeToString(st), base64Encoder.encodeToString(ct));
        }
    }

    private void generatePatternTrapdoors(Map<String, List<String>> keywordPatternTrapdoors, String tripleKeyword, List<String> keywords) {
        List<String> trapdoors;
        int i = 0;
        for (String keyword : keywords) {
            trapdoors = keywordPatternTrapdoors.get(keyword);
            if (trapdoors == null)
                trapdoors = new LinkedList<>();
            trapdoors.add(base64Encoder.encodeToString(generateDETLayer(getDerivedKey(tripleKeyword),
                    tripleKeyword.getBytes(StandardCharsets.UTF_8), SymmetricEncryptionUtils.ivFromInteger(i))));
            keywordPatternTrapdoors.put(keyword, trapdoors);
            i++;
        }
    }

    public byte[] generateRNDLayer(byte[] ciphertext) {
        return SymmetricEncryptionUtils.encrypt(ciphertext, kRND);
    }

    public byte[] generateDETLayer(SecretKey key, byte[] plaintext, byte[] iv) {
        return SymmetricEncryptionUtils.encrypt(plaintext, key, iv);
    }

    public String generateKeywordsFrequencyTrapdoor(String keyword) {
        return base64Encoder.encodeToString(generateDETLayer(getDerivedKey(keyword), keyword.getBytes(StandardCharsets.UTF_8), frequencyIV));
    }

    public String generateTrapdoorAndIncrementIV(String keyword) {
        return base64Encoder.encodeToString(generateDETLayer(getDerivedKey(keyword), keyword.getBytes(StandardCharsets.UTF_8),
                SymmetricEncryptionUtils.ivFromInteger(incrementKeywordFrequency(keyword))));
    }

    public String generateTrapdoor(String keyword, int value) {
        return base64Encoder.encodeToString(generateDETLayer(getDerivedKey(keyword),
                keyword.getBytes(StandardCharsets.UTF_8), SymmetricEncryptionUtils.ivFromInteger(value)));
    }

    private int incrementKeywordFrequency(String keyword) {
        return keywordFrequencies.merge(keyword, 1, Integer::sum);
    }

    public byte[] decryptRNDLayer(byte[] ciphertext) throws AEADBadTagException {
        return SymmetricEncryptionUtils.decrypt(kRND, ciphertext);
    }

    public byte[] decryptDETLayer(byte[] ciphertext) throws AEADBadTagException {
        return SymmetricEncryptionUtils.decrypt(kDET, ciphertext, ivDET);
    }

    private SecretKey getDerivedKey(String context) {
        SecretKey key = derivedKeys.get(context);
        if (key == null) {
            key = SymmetricEncryptionUtils.generateKey(kMASTER, context.getBytes(StandardCharsets.UTF_8));
            derivedKeys.put(context, key);
        }
        return key;
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

    public void clear() {
        keywordFrequencies.clear();
        encryptedNodes.clear();
    }

    public void clearNodes() {
        keywordFrequencies.clear();
        encryptedNodes.clear();
    }

    public void clearFrequencies() {
        keywordFrequencies.clear();
        encryptedNodes.clear();
    }
}
