package pt.fct.nova.id.srv.application.protocols;


import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import pt.fct.nova.id.srv.application.crypto.SymmetricEncryptionUtils;
import pt.fct.nova.id.srv.application.crypto.dgk.DGKKeyPairGenerator;
import pt.fct.nova.id.srv.application.crypto.dgk.DGKUtils;
import pt.fct.nova.id.srv.application.crypto.dgk.HomomorphicException;
import pt.fct.nova.id.srv.application.protocols.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.query.jobs.VariablesPattern;
import pt.fct.nova.id.srv.presentation.controllers.ParsingUtils;

import javax.crypto.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

import static pt.fct.nova.id.srv.application.query.QueryUtils.generateID;
import static pt.fct.nova.id.srv.application.query.jobs.VariablesPattern.*;
import static pt.fct.nova.id.srv.application.query.jobs.VariablesPattern.SPO;

public class Protocol2 implements EncryptionProtocol {

    private final byte[] ivDET;
    private final SecretKey kMASTER, kRND;
    private final PrivateKey privDGK;
    private final PublicKey pubDGK;
    private final Map<String, String> encryptedNodes;
    private final Map<String, Integer> keywordFrequencies;
    private final Map<String, SecretKey> keywordDerivedKeys;
    private final Base64.Decoder base64Decoder;
    private final Base64.Encoder base64Encoder;
    private final String schemaKeyword;

    public Protocol2(SecretKey kMASTER, SecretKey kRND, KeyPair keyPairDGK, byte[] iv, String schemaKeyword) {
        this.ivDET = iv;
        this.kMASTER = kMASTER;
        this.kRND = kRND;
        this.privDGK = keyPairDGK.getPrivate();
        this.pubDGK = keyPairDGK.getPublic();
        this.schemaKeyword = schemaKeyword;
        this.encryptedNodes = new HashMap<>();
        this.keywordFrequencies = new HashMap<>();
        this.keywordDerivedKeys = new HashMap<>();
        this.base64Decoder = Base64.getUrlDecoder();
        this.base64Encoder = Base64.getUrlEncoder();
    }

    public Protocol2() throws HomomorphicException {
        this.ivDET = SymmetricEncryptionUtils.generateRandomIV();
        this.kMASTER = SymmetricEncryptionUtils.generateKey();
        this.kRND = SymmetricEncryptionUtils.generateKey();
        KeyPair keyPairDGK = DGKUtils.generateKeyPair();
        this.privDGK = keyPairDGK.getPrivate();
        this.pubDGK = keyPairDGK.getPublic();
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

    public Map<String, String> getEncryptedNodes() {
        return encryptedNodes;
    }

    public Map<String, Integer> getKeywordFrequencies() {
        return keywordFrequencies;
    }

    @Override
    public void exec(List<Triple> triples, boolean schema) throws InvalidNodeException {
        if (schema)
            encryptSchemaTriples(triples);
        else
            encryptTriples(triples);
        encryptKeywordInfo();
    }

    private void encryptSchemaTriples(List<Triple> triples) throws InvalidNodeException {
        for (Triple t : triples) {
            encodeSchemaNode(ParsingUtils.parseNode(t.getSubject()));
            encodeSchemaNode(ParsingUtils.parseNode(t.getPredicate()));
            encodeSchemaNode(ParsingUtils.parseNode(t.getObject()));
        }
    }

    private int encodeSchemaNode(String node) {
        int frequency = incrementKeywordFrequency(schemaKeyword);
        byte[] st = generateDETLayer(getKeywordDerivedKey(schemaKeyword), schemaKeyword.getBytes(StandardCharsets.UTF_8), SymmetricEncryptionUtils.ivFromInteger(frequency));
        byte[] ct = generateRNDLayer(node.getBytes(StandardCharsets.UTF_8));
        encryptedNodes.put(base64Encoder.encodeToString(st), base64Encoder.encodeToString(ct));
        return frequency;
    }


    private void encryptTriples(List<Triple> triples) throws InvalidNodeException {
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

    private int encodeNode(String node, VariablesPattern pattern, String keyword) {
        keyword = ParsingUtils.generateKeyword(pattern, keyword);
        int frequency = incrementKeywordFrequency(keyword);
        byte[] st = generateDETLayer(getKeywordDerivedKey(keyword), keyword.getBytes(StandardCharsets.UTF_8), SymmetricEncryptionUtils.ivFromInteger(frequency));
        //TODO: Generate eqTags;
        byte[] eqTag = generateEQTag(node);
        byte[] ct = generateRNDLayer(node.getBytes(StandardCharsets.UTF_8));
        encryptedNodes.put(base64Encoder.encodeToString(st), base64Encoder.encodeToString(eqTag));
        encryptedNodes.put(base64Encoder.encodeToString(eqTag), base64Encoder.encodeToString(ct));
        return frequency;
    }

    private byte[] generateEQTag(String node) {

    }

    private void encodeTriple(String keyword, List<Integer> frequencies) {
        keyword = ParsingUtils.generateKeyword(SPO, keyword);
        byte[] st;
        int i = 0;
        for (int f : frequencies) {
            st = generateDETLayer(getKeywordDerivedKey(keyword), keyword.getBytes(StandardCharsets.UTF_8), SymmetricEncryptionUtils.ivFromInteger(i));
            encryptedNodes.put(base64Encoder.encodeToString(st), base64Encoder.encodeToString(generateRNDLayer(ParsingUtils.integerToByteArray(f))));
            i++;
        }
    }

    public Map<String, List<String>> generateKeywordsPatternTrapdoors(List<Triple> triples) throws InvalidNodeException {
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

    private void encryptKeywordInfo() {
        for (String keyword : keywordFrequencies.keySet()) {
            byte[] st = generateDETLayer(keywordDerivedKeys.get(keyword), keyword.getBytes(StandardCharsets.UTF_8), SymmetricEncryptionUtils.generateZeroFilledIV());
            byte[] ct = generateRNDLayer(ParsingUtils.integerToByteArray(keywordFrequencies.get(keyword)));
            encryptedNodes.put(
                    base64Encoder.encodeToString(st),
                    base64Encoder.encodeToString(ct)
            );
        }
    }

    private void generatePatternTrapdoors(Map<String, List<String>> keywordPatternTrapdoors, String tripleKeyword, List<String> keywords) {
        tripleKeyword = ParsingUtils.generateKeyword(SPO, tripleKeyword);
        List<String> trapdoors;
        int i = 0;
        for (String keyword : keywords) {
            trapdoors = keywordPatternTrapdoors.get(keyword);
            if (trapdoors == null)
                trapdoors = new LinkedList<>();
            trapdoors.add(base64Encoder.encodeToString(generateDETLayer(getKeywordDerivedKey(tripleKeyword),
                    tripleKeyword.getBytes(StandardCharsets.UTF_8), SymmetricEncryptionUtils.ivFromInteger(i))));
            keywordPatternTrapdoors.put(keyword, trapdoors);
            i++;
        }
    }

    public byte[] generateRNDLayer(byte[] deterministicCiphertext) {
        return SymmetricEncryptionUtils.encrypt(deterministicCiphertext, kRND);
    }

    public byte[] generateDETLayer(SecretKey key, byte[] plaintext, byte[] iv) {
        return SymmetricEncryptionUtils.encrypt(plaintext, key, iv);
    }

    public String generateKeywordsFrequencyTrapdoor(String keyword) {
        return base64Encoder.encodeToString(generateDETLayer(getKeywordDerivedKey(keyword), keyword.getBytes(StandardCharsets.UTF_8), SymmetricEncryptionUtils.generateZeroFilledIV()));
    }

    public String generateTrapdoorAndIncrementIV(String keyword) {
        return base64Encoder.encodeToString(generateDETLayer(getKeywordDerivedKey(keyword),
                keyword.getBytes(StandardCharsets.UTF_8), SymmetricEncryptionUtils.ivFromInteger(incrementKeywordFrequency(keyword))));
    }

    private int incrementKeywordFrequency(String keyword) {
        return keywordFrequencies.merge(keyword, 1, Integer::sum);
    }

    public String generateTrapdoor(String keyword, int value) {
        return base64Encoder.encodeToString(generateDETLayer(getKeywordDerivedKey(keyword), keyword.getBytes(StandardCharsets.UTF_8), SymmetricEncryptionUtils.ivFromInteger(value)));
    }

    public byte[] decryptRNDLayer(String ciphertext) throws AEADBadTagException {
        return SymmetricEncryptionUtils.decrypt(kRND, base64Decoder.decode(ciphertext));
    }

    public byte[] decryptDETLayer(String ciphertext) throws AEADBadTagException {
        return SymmetricEncryptionUtils.decrypt(kDET, base64Decoder.decode(ciphertext), ivDET);
    }

    private SecretKey getKeywordDerivedKey(String keyword) {
        SecretKey keywordDerivedKey = keywordDerivedKeys.get(keyword);
        if (keywordDerivedKey == null) {
            keywordDerivedKey = SymmetricEncryptionUtils.generateKey(kMASTER, keyword.getBytes(StandardCharsets.UTF_8));
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

    public PrivateKey getPrivDGK() {
        return privDGK;
    }

    public PublicKey getPubDGK() {
        return pubDGK;
    }
}
