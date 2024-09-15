package pt.fct.nova.id.srv.application.schemes;


import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import pt.fct.nova.id.srv.application.crypto.SymmetricEncryptionUtils;
import pt.fct.nova.id.srv.application.crypto.dgk.*;
import pt.fct.nova.id.srv.application.schemes.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.presentation.controllers.ParsingUtils;

import javax.crypto.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;


import static pt.fct.nova.id.srv.application.query.QueryUtils.generateID;
import static pt.fct.nova.id.srv.application.query.jobs.VariablesPattern.*;
import static pt.fct.nova.id.srv.application.query.jobs.VariablesPattern.SPO;

public class EncryptionSchemeV2 implements EncryptionScheme {
    private final byte[] ivDET;
    private final SecretKey kMASTER, kRND;
    private final PrivateKey privDGK;
    private final PublicKey pubDGK;
    private final Map<String, String> encryptedNodes;
    private final Map<String, Integer> keywordFrequencies;
    private final Map<String, SecretKey> derivedKeys;
    private final Map<String, Integer> eqTags;
    private final String schemaKeyword;
    private final byte[] zeroIV;
    private long lastEqTag;

    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder();

    public EncryptionSchemeV2(SecretKey kMASTER, SecretKey kRND, KeyPair keyPairDGK, byte[] iv, String schemaKeyword, long lastEqTag) {
        this.ivDET = iv;
        this.kMASTER = kMASTER;
        this.kRND = kRND;
        this.privDGK = keyPairDGK.getPrivate();
        this.pubDGK = keyPairDGK.getPublic();
        this.schemaKeyword = schemaKeyword;
        this.encryptedNodes = new HashMap<>();
        this.keywordFrequencies = new HashMap<>();
        this.derivedKeys = new HashMap<>();
        this.eqTags = new HashMap<>();
        this.zeroIV = SymmetricEncryptionUtils.generateZeroFilledIV();
        this.lastEqTag = lastEqTag;
    }

    public EncryptionSchemeV2() throws HomomorphicException {
        this.ivDET = SymmetricEncryptionUtils.generateRandomIV();
        this.kMASTER = SymmetricEncryptionUtils.generateKey();
        this.kRND = SymmetricEncryptionUtils.generateKey();
        KeyPair keyPairDGK = DGKUtils.generateKeyPair();
        this.privDGK = keyPairDGK.getPrivate();
        this.pubDGK = keyPairDGK.getPublic();
        this.schemaKeyword = generateID();
        this.encryptedNodes = new HashMap<>();
        this.keywordFrequencies = new HashMap<>();
        this.derivedKeys = new HashMap<>();
        this.eqTags = new HashMap<>();
        this.zeroIV = SymmetricEncryptionUtils.generateZeroFilledIV();
        this.lastEqTag = 0L;
        System.out.println("Generated secrets.");
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

    public String getSchemaKeyword() {
        return schemaKeyword;
    }

    public PrivateKey getPrivDGK() {
        return privDGK;
    }

    public PublicKey getPubDGK() {
        return pubDGK;
    }

    public Map<String, String> getEncryptedNodes() {
        return encryptedNodes;
    }

    public Map<String, Integer> getKeywordFrequencies() {
        return keywordFrequencies;
    }

    @Override
    public void encrypt(Set<Triple> triples, boolean schema) throws InvalidNodeException {
        if (schema)
            encryptSchemaTriples(triples);
        else
            encryptTriples(triples);
        encryptContextualData();
    }

    private void encryptSchemaTriples(Set<Triple> triples) throws InvalidNodeException {
        for (Triple t : triples) {
            encodeSchemaNode(ParsingUtils.parseNode(t.getSubject()));
            encodeSchemaNode(ParsingUtils.parseNode(t.getPredicate()));
            encodeSchemaNode(ParsingUtils.parseNode(t.getObject()));
        }
    }

    private void encodeSchemaNode(String node) {
        int frequency = incrementKeywordFrequency(schemaKeyword);
        byte[] st = encryptDET(getDerivedKey(schemaKeyword), schemaKeyword.getBytes(StandardCharsets.UTF_8), SymmetricEncryptionUtils.ivFromInteger(frequency));
        byte[] ct = encryptRND(node.getBytes(StandardCharsets.UTF_8));
        encryptedNodes.put(base64Encoder.encodeToString(st), base64Encoder.encodeToString(ct));
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
                frequencies.add(encryptNode(parsed_p, ParsingUtils.generateKeyword(PO, s_keyword)));
                frequencies.add(encryptNode(parsed_o, ParsingUtils.generateKeyword(PO, s_keyword)));
                frequencies.add(encryptNode(parsed_s, ParsingUtils.generateKeyword(SO, p_keyword)));
                frequencies.add(encryptNode(parsed_o, ParsingUtils.generateKeyword(SO, p_keyword)));
                frequencies.add(encryptNode(parsed_s, ParsingUtils.generateKeyword(SP, o_keyword)));
                frequencies.add(encryptNode(parsed_p, ParsingUtils.generateKeyword(SP, o_keyword)));
                frequencies.add(encryptNode(parsed_s, ParsingUtils.generateKeyword(S, String.format(COMPOUND_KEYWORD, p_keyword, o_keyword))));
                frequencies.add(encryptNode(parsed_p, ParsingUtils.generateKeyword(P, String.format(COMPOUND_KEYWORD, s_keyword, o_keyword))));
                frequencies.add(encryptNode(parsed_o, ParsingUtils.generateKeyword(O, String.format(COMPOUND_KEYWORD, s_keyword, p_keyword))));
                encryptTriple(ParsingUtils.generateKeyword(SPO, t_keyword), frequencies);
            }
        }
    }

    private int encryptNode(String node, String keyword) {
        int frequency = incrementKeywordFrequency(keyword);
        byte[] st = encryptDET(getDerivedKey(keyword), keyword.getBytes(StandardCharsets.UTF_8), SymmetricEncryptionUtils.ivFromInteger(frequency));
        byte[] eqTag = encryptEQ(node);
        byte[] ct = encryptRND(node.getBytes(StandardCharsets.UTF_8));
        encryptedNodes.put(base64Encoder.encodeToString(st), base64Encoder.encodeToString(eqTag));
        encryptedNodes.put(base64Encoder.encodeToString(eqTag), base64Encoder.encodeToString(ct));
        return frequency;
    }


    private void encryptTriple(String keyword, List<Integer> frequencies) {
        byte[] st;
        int i = 0;
        for (int f : frequencies) {
            st = encryptDET(getDerivedKey(keyword), keyword.getBytes(StandardCharsets.UTF_8), SymmetricEncryptionUtils.ivFromInteger(i));
            encryptedNodes.put(base64Encoder.encodeToString(st), base64Encoder.encodeToString(encryptRND(ParsingUtils.integerToByteArray(f))));
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

    private void encryptContextualData() {
        byte[] st, ct;
        for (String keyword : keywordFrequencies.keySet()) {
            st = encryptDET(derivedKeys.get(keyword), keyword.getBytes(StandardCharsets.UTF_8), zeroIV);
            ct = encryptRND(ParsingUtils.integerToByteArray(keywordFrequencies.get(keyword)));
            encryptedNodes.put(base64Encoder.encodeToString(st), base64Encoder.encodeToString(ct));
        }

        for (String node : eqTags.keySet()) {
            st = encryptDET(derivedKeys.get(node), node.getBytes(StandardCharsets.UTF_8), zeroIV);
            ct = encryptRND(ParsingUtils.integerToByteArray(eqTags.get(node)));
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
            trapdoors.add(base64Encoder.encodeToString(encryptDET(getDerivedKey(tripleKeyword),
                    tripleKeyword.getBytes(StandardCharsets.UTF_8), SymmetricEncryptionUtils.ivFromInteger(i))));
            keywordPatternTrapdoors.put(keyword, trapdoors);
            i++;
        }
    }

    public byte[] encryptRND(byte[] ciphertext) {
        return SymmetricEncryptionUtils.encrypt(ciphertext, kRND);
    }

    private byte[] encryptEQ(String node) {
        return ((DGKPublicKey) pubDGK).encrypt(getEqTag(node)).toByteArray();
    }

    public byte[] encryptDET(SecretKey key, byte[] plaintext, byte[] iv) {
        return SymmetricEncryptionUtils.encrypt(plaintext, key, iv);
    }

    public String generateTrapdoor(String node) {
        return base64Encoder.encodeToString(encryptDET(getDerivedKey(node), node.getBytes(StandardCharsets.UTF_8), zeroIV));
    }

    public String generateTrapdoor(String keyword, int value) {
        return base64Encoder.encodeToString(encryptDET(getDerivedKey(keyword),
                keyword.getBytes(StandardCharsets.UTF_8), SymmetricEncryptionUtils.ivFromInteger(value)));
    }

    public String generateTrapdoorAndIncrementIV(String keyword) {
        return  base64Encoder.encodeToString(encryptDET(getDerivedKey(keyword), keyword.getBytes(StandardCharsets.UTF_8),
                SymmetricEncryptionUtils.ivFromInteger(incrementKeywordFrequency(keyword))));
    }

    private int incrementKeywordFrequency(String keyword) {
        return keywordFrequencies.merge(keyword, 1, Integer::sum);
    }

    private long getEqTag(String node) {
        long eqTag = Math.toIntExact(eqTags.computeIfAbsent(node, k -> Math.toIntExact(lastEqTag + 1)));
        if (eqTag > lastEqTag)
            lastEqTag = eqTag;
        return eqTag;
    }

    public byte[] decryptRNDLayer(byte[] ciphertext) throws AEADBadTagException {
        return SymmetricEncryptionUtils.decrypt(kRND, ciphertext);
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
        int frequency;
        for (String keyword : values.keySet()) {
            frequency = values.get(keyword);
            if (frequency > 0)
                keywordFrequencies.put(keyword, frequency);
        }
    }

    public void setEqTags(Map<String, Integer> values) {
        eqTags.putAll(values);
    }


    public DGKEqKey getEqKey() {
        return new DGKEqKey(
                ((DGKPrivateKey) privDGK).getP(),
                ((DGKPrivateKey) privDGK).getVp(),
                ((DGKPublicKey) pubDGK).getN()
        );
    }

    public Long getLastEqTag() {
        return this.lastEqTag;
    }

    public void clearEqTags() {
        eqTags.clear();
    }

    public void clearNodes() {
        encryptedNodes.clear();
    }

    public void clearFrequencies() {
        keywordFrequencies.clear();
    }
}
