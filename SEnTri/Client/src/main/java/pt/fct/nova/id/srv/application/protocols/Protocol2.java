package pt.fct.nova.id.srv.application.protocols;


import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import pt.fct.nova.id.srv.application.crypto.SymmetricEncryptionUtils;
import pt.fct.nova.id.srv.application.crypto.dgk.*;
import pt.fct.nova.id.srv.application.protocols.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.presentation.controllers.ParsingUtils;

import javax.crypto.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

import static pt.fct.nova.id.srv.application.query.QueryUtils.generateBinaryID;
import static pt.fct.nova.id.srv.application.query.jobs.VariablesPattern.*;
import static pt.fct.nova.id.srv.application.query.jobs.VariablesPattern.SPO;

public class Protocol2 implements EncryptionProtocol {
    private final byte[] ivDET;
    private final SecretKey kMASTER, kRND;
    private final PrivateKey privDGK;
    private final PublicKey pubDGK;
    private final Map<byte[], byte[]> encryptedNodes;
    private final Map<Bytes, Integer> keywordFrequencies;
    private final Map<Bytes, SecretKey> derivedKeys;
    private final Map<Bytes, Integer> eqTags;
    private final byte[] schemaKeyword;
    private final byte[] zeroIV;
    private long lastEqTag;

    public Protocol2(SecretKey kMASTER, SecretKey kRND, KeyPair keyPairDGK, byte[] iv, byte[] schemaKeyword, long lastEqTag) {
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

    public Protocol2() throws HomomorphicException {
        this.ivDET = SymmetricEncryptionUtils.generateRandomIV();
        this.kMASTER = SymmetricEncryptionUtils.generateKey();
        this.kRND = SymmetricEncryptionUtils.generateKey();
        KeyPair keyPairDGK = DGKUtils.generateKeyPair();
        this.privDGK = keyPairDGK.getPrivate();
        this.pubDGK = keyPairDGK.getPublic();
        this.schemaKeyword = generateBinaryID();
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

    public byte[] getSchemaKeyword() {
        return schemaKeyword;
    }

    public PrivateKey getPrivDGK() {
        return privDGK;
    }

    public PublicKey getPubDGK() {
        return pubDGK;
    }

    public Map<byte[], byte[]> getEncryptedNodes() {
        return encryptedNodes;
    }

    public void clear() {
        encryptedNodes.clear();
        keywordFrequencies.clear();
        eqTags.clear();
    }


    public Map<Bytes, Integer> getKeywordFrequencies() {
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
        byte[] st = encryptDET(getDerivedKey(schemaKeyword), schemaKeyword, SymmetricEncryptionUtils.ivFromInteger(frequency));
        byte[] ct = encryptRND(node.getBytes(StandardCharsets.UTF_8));
        encryptedNodes.put(st, ct);
        return frequency;
    }


    private void encryptTriples(Set<Triple> triples) throws InvalidNodeException {
        Node s, p, o;
        byte[] parsed_s, parsed_p, parsed_o;
        String s_keyword, p_keyword, o_keyword, t_keyword;
        List<Integer> frequencies;
        Set<String> processed = new HashSet<>();
        for (Triple t : triples) {
            frequencies = new ArrayList<>(9);
            s = t.getSubject();
            p = t.getPredicate();
            o = t.getObject();
            parsed_s = ParsingUtils.parseNode(s).getBytes(StandardCharsets.UTF_8);
            parsed_p = ParsingUtils.parseNode(p).getBytes(StandardCharsets.UTF_8);
            parsed_o = ParsingUtils.parseNode(o).getBytes(StandardCharsets.UTF_8);
            s_keyword = ParsingUtils.parseKeyword(s);
            p_keyword = ParsingUtils.parseKeyword(p);
            o_keyword = ParsingUtils.parseKeyword(o);
            t_keyword = String.format(TRIPLE_KEYWORD, s_keyword, p_keyword, o_keyword);
            if (!processed.contains(t_keyword)) {
                processed.add(t_keyword);
                frequencies.add(encodeNode(parsed_p, ParsingUtils.generateKeyword(PO, s_keyword).getBytes(StandardCharsets.UTF_8)));
                frequencies.add(encodeNode(parsed_o, ParsingUtils.generateKeyword(PO, s_keyword).getBytes(StandardCharsets.UTF_8)));
                frequencies.add(encodeNode(parsed_s, ParsingUtils.generateKeyword(SO, p_keyword).getBytes(StandardCharsets.UTF_8)));
                frequencies.add(encodeNode(parsed_o, ParsingUtils.generateKeyword(SO, p_keyword).getBytes(StandardCharsets.UTF_8)));
                frequencies.add(encodeNode(parsed_s, ParsingUtils.generateKeyword(SP, o_keyword).getBytes(StandardCharsets.UTF_8)));
                frequencies.add(encodeNode(parsed_p, ParsingUtils.generateKeyword(SP, o_keyword).getBytes(StandardCharsets.UTF_8)));
                frequencies.add(encodeNode(parsed_s, ParsingUtils.generateKeyword(S, String.format(COMPOUND_KEYWORD, p_keyword, o_keyword)).getBytes(StandardCharsets.UTF_8)));
                frequencies.add(encodeNode(parsed_p, ParsingUtils.generateKeyword(P, String.format(COMPOUND_KEYWORD, s_keyword, o_keyword)).getBytes(StandardCharsets.UTF_8)));
                frequencies.add(encodeNode(parsed_o, ParsingUtils.generateKeyword(O, String.format(COMPOUND_KEYWORD, s_keyword, p_keyword)).getBytes(StandardCharsets.UTF_8)));
                encodeTriple(ParsingUtils.generateKeyword(SPO, t_keyword).getBytes(StandardCharsets.UTF_8), frequencies);
            }
        }
    }

    private int encodeNode(byte[] node, byte[] keyword) {
        int frequency = incrementKeywordFrequency(keyword);
        byte[] st = encryptDET(getDerivedKey(keyword), keyword, SymmetricEncryptionUtils.ivFromInteger(frequency));
        byte[] eqTag = encryptEQ(node);
        byte[] ct = encryptRND(node);
        encryptedNodes.put(st, eqTag);
        encryptedNodes.put(eqTag, ct);
        return frequency;
    }


    private void encodeTriple(byte[] keyword, List<Integer> frequencies) {
        byte[] st;
        int i = 0;
        for (int f : frequencies) {
            st = encryptDET(getDerivedKey(keyword), keyword, SymmetricEncryptionUtils.ivFromInteger(i));
            encryptedNodes.put(st, encryptRND(ParsingUtils.integerToByteArray(f)));
            i++;
        }
    }

    public Map<Bytes, List<byte[]>> generateKeywordsPatternTrapdoors(Set<Triple> triples) throws InvalidNodeException {
        Map<Bytes, List<byte[]>> res = new HashMap<>(triples.size() * 6);
        String s, p, o, t_keyword;
        List<Bytes> keywords;
        Set<String> processed = new HashSet<>();
        for (Triple t : triples) {
            keywords = new ArrayList<>(9);
            s = ParsingUtils.parseKeyword(t.getSubject());
            p = ParsingUtils.parseKeyword(t.getPredicate());
            o = ParsingUtils.parseKeyword(t.getObject());
            t_keyword = String.format(TRIPLE_KEYWORD, s, p, o);
            if (!processed.contains(t_keyword)) {
                processed.add(t_keyword);
                keywords.add(new Bytes(ParsingUtils.generateKeyword(PO, s).getBytes(StandardCharsets.UTF_8)));
                keywords.add(new Bytes(ParsingUtils.generateKeyword(PO, s).getBytes(StandardCharsets.UTF_8)));
                keywords.add(new Bytes(ParsingUtils.generateKeyword(SO, p).getBytes(StandardCharsets.UTF_8)));
                keywords.add(new Bytes(ParsingUtils.generateKeyword(SO, p).getBytes(StandardCharsets.UTF_8)));
                keywords.add(new Bytes(ParsingUtils.generateKeyword(SP, o).getBytes(StandardCharsets.UTF_8)));
                keywords.add(new Bytes(ParsingUtils.generateKeyword(SP, o).getBytes(StandardCharsets.UTF_8)));
                keywords.add(new Bytes(ParsingUtils.generateKeyword(S, p, o).getBytes(StandardCharsets.UTF_8)));
                keywords.add(new Bytes(ParsingUtils.generateKeyword(P, s, o).getBytes(StandardCharsets.UTF_8)));
                keywords.add(new Bytes(ParsingUtils.generateKeyword(O, s, p).getBytes(StandardCharsets.UTF_8)));
                generatePatternTrapdoors(res, ParsingUtils.generateKeyword(SPO, t_keyword).getBytes(StandardCharsets.UTF_8), keywords);
            }
        }
        System.out.println("KeywordsPatternTrapdoors: " + res.size());
        return res;
    }

    private void encryptKeywordInfo() {
        byte[] st, ct;
        for (Bytes keyword : keywordFrequencies.keySet()) {
            st = encryptDET(derivedKeys.get(keyword), keyword.getData(), zeroIV);
            ct = encryptRND(ParsingUtils.integerToByteArray(keywordFrequencies.get(keyword)));
            encryptedNodes.put(st, ct);
        }

        for (Bytes node : eqTags.keySet()) {
            st = encryptDET(derivedKeys.get(node), node.getData(), zeroIV);
            ct = encryptRND(ParsingUtils.integerToByteArray(eqTags.get(node)));
            encryptedNodes.put(st, ct);
        }
    }

    private void generatePatternTrapdoors(Map<Bytes, List<byte[]>> keywordPatternTrapdoors, byte[] tripleKeyword, List<Bytes> keywords) {
        List<byte[]> trapdoors;
        int i = 0;
        for (Bytes keyword : keywords) {
            trapdoors = keywordPatternTrapdoors.get(keyword);
            if (trapdoors == null)
                trapdoors = new LinkedList<>();
            trapdoors.add(encryptDET(getDerivedKey(tripleKeyword), tripleKeyword, SymmetricEncryptionUtils.ivFromInteger(i)));
            keywordPatternTrapdoors.put(keyword, trapdoors);
            i++;
        }
    }

    public byte[] encryptRND(byte[] ciphertext) {
        return SymmetricEncryptionUtils.encrypt(ciphertext, kRND);
    }

    private byte[] encryptEQ(byte[] node) {
        return ((DGKPublicKey) pubDGK).encrypt(getEqTag(node)).toByteArray();
    }

    public byte[] encryptDET(SecretKey key, byte[] plaintext, byte[] iv) {
        return SymmetricEncryptionUtils.encrypt(plaintext, key, iv);
    }

    public byte[] generateKeywordsEqTagTrapdoor(byte[] node) {
        return encryptDET(getDerivedKey(node), node, zeroIV);
    }

    public byte[] generateKeywordsFrequencyTrapdoor(byte[] keyword) {
        return encryptDET(getDerivedKey(keyword), keyword, zeroIV);
    }

    public byte[] generateTrapdoorAndIncrementIV(byte[] keyword) {
        return encryptDET(getDerivedKey(keyword), keyword, SymmetricEncryptionUtils.ivFromInteger(incrementKeywordFrequency(keyword)));
    }

    private int incrementKeywordFrequency(byte[] keyword) {
        return keywordFrequencies.merge(new Bytes(keyword), 1, Integer::sum);
    }

    private long getEqTag(byte[] node) {
        long eqTag = Math.toIntExact(eqTags.computeIfAbsent(new Bytes(node), k -> Math.toIntExact(lastEqTag + 1)));
        if (eqTag > lastEqTag)
            lastEqTag = eqTag;
        return eqTag;
    }

    public byte[] generateTrapdoor(byte[] keyword, int value) {
        return encryptDET(getDerivedKey(keyword), keyword, SymmetricEncryptionUtils.ivFromInteger(value));
    }

    public byte[] decryptRNDLayer(byte[] ciphertext) throws AEADBadTagException {
        return SymmetricEncryptionUtils.decrypt(kRND, ciphertext);
    }

    private SecretKey getDerivedKey(byte[] context) {
        Bytes bytes = new Bytes(context);
        SecretKey key = derivedKeys.get(bytes);
        if (key == null) {
            key = SymmetricEncryptionUtils.generateKey(kMASTER, context);
            derivedKeys.put(bytes, key);
        }
        return key;
    }

    public void setKeywordFrequencies(Map<Bytes, Integer> values) {
        keywordFrequencies.clear();
        int frequency;
        for (Bytes keyword : values.keySet()) {
            frequency = values.get(keyword);
            if (frequency > 0)
                keywordFrequencies.put(keyword, frequency);
        }
    }

    public void seEqTags(Map<Bytes, Integer> values) {
        eqTags.putAll(values);
    }


    public DGKEqKey getEqKey() {
        return new DGKEqKey(
                ((DGKPrivateKey) privDGK).getP(),
                ((DGKPrivateKey) privDGK).getVp(),
                ((DGKPublicKey) pubDGK).getN(),
                ((DGKPublicKey) pubDGK).getU()
        );
    }

    public Long getLastEqTag() {
        return this.lastEqTag;
    }
}
