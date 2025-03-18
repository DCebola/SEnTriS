package pt.fct.nova.id.srv.presentation.controllers;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.lang.CollectorStreamTriples;
import org.apache.jena.sparql.core.Var;
import pt.fct.nova.id.srv.application.crypto.SymmetricEncryptionUtils;
import pt.fct.nova.id.srv.application.crypto.dgk.DGKEqKey;
import pt.fct.nova.id.srv.application.crypto.dgk.DGKUtils;
import pt.fct.nova.id.srv.application.query.execution.DefaultSPARQLResult;
import pt.fct.nova.id.srv.application.query.execution.SPARQLResult;
import pt.fct.nova.id.srv.application.query.jobs.VariablesPattern;
import pt.fct.nova.id.srv.application.query.plans.DefaultQueryExecutionPlan;
import pt.fct.nova.id.srv.application.crypto.schemes.*;
import pt.fct.nova.id.srv.application.crypto.schemes.exceptions.*;
import pt.fct.nova.id.srv.presentation.dtos.AuthForm;
import pt.fct.nova.id.srv.presentation.dtos.RequestDecisionForm;
import pt.fct.nova.id.srv.presentation.dtos.Role;
import pt.fct.nova.id.srv.presentation.exceptions.UnknownRDFLanguageException;

import javax.crypto.SecretKey;
import java.io.*;
import java.math.BigInteger;
import java.security.KeyPair;
import java.util.*;

import static pt.fct.nova.id.srv.application.query.jobs.VariablesPattern.*;
import static pt.fct.nova.id.srv.application.crypto.schemes.EncryptionScheme.*;
import static pt.fct.nova.id.srv.presentation.controllers.EncryptedTriplestoreV1Controller.*;
import static pt.fct.nova.id.srv.presentation.controllers.EncryptedTriplestoreV2Controller.*;

public class ParsingUtils {
    public static final String COOKIE_PARAM = "session";
    public static final String BASIC_SEPARATOR = System.getenv("BASIC_SEPARATOR");
    public static final String NODE_SEPARATOR = System.getenv("NODE_SEPARATOR");
    private static final String BLANK_PREFIX = "B";
    private static final String SIMPLE_PREFIX = "S";
    private static final String LITERAL_PREFIX = "L";
    private final static String BLANK_NODE = BLANK_PREFIX.concat(NODE_SEPARATOR).concat("%s");
    private static final String SIMPLE_NODE = SIMPLE_PREFIX.concat(NODE_SEPARATOR).concat("%s");
    private static final String LITERAL_NODE = LITERAL_PREFIX.concat(NODE_SEPARATOR).concat("%s").concat(NODE_SEPARATOR).concat("%s");
    private static final int PREFIX_POS = 0;
    private static final int VALUE_POS = 1;
    private static final int LITERAL_DATATYPE_POS = 2;

    private static final String BLANK = "BLANK";
    private static final Base64.Decoder base64Decoder = Base64.getUrlDecoder();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder();

    public static HttpEntity generateTriplestoreForm(String username, String triplestoreID) {
        return MultipartEntityBuilder.create()
                .addTextBody("issuer", username, ContentType.TEXT_PLAIN)
                .addTextBody("triplestoreID", triplestoreID, ContentType.TEXT_PLAIN)
                .build();
    }

    public static HttpEntity credentialsFormToHttpEntity(AuthForm credentialsForm) {
        return MultipartEntityBuilder.create()
                .addTextBody("username", credentialsForm.getUsername(), ContentType.TEXT_PLAIN)
                .addTextBody("password", credentialsForm.getPassword(), ContentType.TEXT_PLAIN)
                .build();
    }

    public static HttpEntity requestDecisionFormToHttpEntity(RequestDecisionForm decisionForm) {
        return MultipartEntityBuilder.create()
                .addTextBody("target", decisionForm.getTarget(), ContentType.TEXT_PLAIN)
                .addTextBody("accept", Boolean.toString(decisionForm.isAccept()), ContentType.TEXT_PLAIN)
                .build();
    }

    public static HttpEntity generateRoleRequest(String issuer, Role role) {
        return MultipartEntityBuilder.create()
                .addTextBody("issuer", issuer, ContentType.TEXT_PLAIN)
                .addTextBody("role", role.name(), ContentType.TEXT_PLAIN)
                .build();
    }


    public static HttpEntity mapOfStringsStringsToHttpEntity(Map<String, String> map) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(map);
            return new ByteArrayEntity(bos.toByteArray(), ContentType.APPLICATION_OCTET_STREAM);
        }
    }


    public static HttpEntity triplesSetToHttpEntity(Set<Triple> triples) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(triples);
            return new ByteArrayEntity(bos.toByteArray(), ContentType.APPLICATION_OCTET_STREAM);
        }
    }

    public static Set<Triple> parseSchema(String schema) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(base64Decoder.decode(schema));
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (Set<Triple>) ois.readObject();
        }
    }

    public static HttpEntity stringSetToHttpEntity(Set<String> set) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(set);
            return new ByteArrayEntity(bos.toByteArray(), ContentType.APPLICATION_OCTET_STREAM);
        }
    }

    public static HttpEntity stringListToHttpEntity(List<String> list) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(list);
            return new ByteArrayEntity(bos.toByteArray(), ContentType.APPLICATION_OCTET_STREAM);
        }
    }

    public static HttpEntity generateUpdateRequest(List<String> deletions, List<String> uploads) throws IOException {
        try (ByteArrayOutputStream bos_deletions = new ByteArrayOutputStream(); ObjectOutputStream oos_deletions = new ObjectOutputStream(bos_deletions);
             ByteArrayOutputStream bos_uploads = new ByteArrayOutputStream(); ObjectOutputStream oos_uploads = new ObjectOutputStream(bos_uploads)) {
            oos_deletions.writeObject(deletions);
            oos_uploads.writeObject(uploads);
            return MultipartEntityBuilder.create()
                    .addBinaryBody("deletions", bos_deletions.toByteArray())
                    .addBinaryBody("uploads", bos_uploads.toByteArray())
                    .build();
        }
    }

    public static HttpEntity queryExecutionPlanToHttpEntity(DefaultQueryExecutionPlan plan) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(plan);
            return new ByteArrayEntity(bos.toByteArray(), ContentType.APPLICATION_OCTET_STREAM);
        }
    }

    public static HttpEntity generateSecureQueryRequest(byte[] keyBytes, DefaultQueryExecutionPlan plan) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(plan);
            return MultipartEntityBuilder.create()
                    .addBinaryBody("key", keyBytes)
                    .addBinaryBody("queryExecutionPlan", bos.toByteArray())
                    .build();
        }

    }

    public static HttpEntity generateV2SearchRequest(List<String> trapdoors, BigInteger mask, BigInteger n) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(trapdoors);
            return MultipartEntityBuilder.create()
                    .addBinaryBody("mask", mask.toByteArray())
                    .addBinaryBody("n", n.toByteArray())
                    .addBinaryBody("trapdoors", bos.toByteArray())
                    .build();
        }
    }

    public static Map<String, String> parseSecretsMap(String secrets) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream is = new ByteArrayInputStream(base64Decoder.decode(secrets));
             ObjectInputStream ois = new ObjectInputStream(is)) {
            return (Map<String, String>) ois.readObject();
        }
    }

    public static List<byte[]> parseListOfBytes(String results) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream is = new ByteArrayInputStream(base64Decoder.decode(results));
             ObjectInputStream ois = new ObjectInputStream(is)) {
            return (List<byte[]>) ois.readObject();
        }
    }

    public static SPARQLResult parseSPARQLResult(String results) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream is = new ByteArrayInputStream(base64Decoder.decode(results));
             ObjectInputStream ois = new ObjectInputStream(is)) {
            return (DefaultSPARQLResult) ois.readObject();
        }
    }

    public static EncryptionSchemeV1 getProtocol1(Map<String, String> secrets) {
        SecretKey k1 = SymmetricEncryptionUtils.parseKey(base64Decoder.decode(secrets.get(String.format(SECRETS_KEY, 1))));
        SecretKey k2 = SymmetricEncryptionUtils.parseKey(base64Decoder.decode(secrets.get(String.format(SECRETS_KEY, 2))));
        SecretKey k3 = SymmetricEncryptionUtils.parseKey(base64Decoder.decode(secrets.get(String.format(SECRETS_KEY, 3))));
        byte[] iv = base64Decoder.decode(secrets.get(SECRETS_IV));
        String schemaKeyword = secrets.get(SECRETS_SCHEMA_KEYWORD);
        return new EncryptionSchemeV1(k1, k2, k3, iv, schemaKeyword);
    }

    public static EncryptionSchemeV2 getProtocol2(Map<String, String> secrets) throws IOException, ClassNotFoundException {
        SecretKey k1 = SymmetricEncryptionUtils.parseKey(base64Decoder.decode(secrets.get(String.format(SECRETS_KEY, 1))));
        SecretKey k2 = SymmetricEncryptionUtils.parseKey(base64Decoder.decode(secrets.get(String.format(SECRETS_KEY, 2))));
        KeyPair keyPair = DGKUtils.parseKeyPair(base64Decoder.decode(secrets.get(SECRETS_KEY_PAIR)));
        byte[] iv = base64Decoder.decode(secrets.get(SECRETS_IV));
        String schemaKeyword = secrets.get(SECRETS_SCHEMA_KEYWORD);
        long lastEqTag = byteArrayToInteger(base64Decoder.decode(secrets.get(SECRETS_LAST_EQ_TAG)));
        System.out.println("Retrieved protocol 2.");
        return new EncryptionSchemeV2(k1, k2, keyPair, iv, schemaKeyword, lastEqTag);
    }

    public static Map<String, String> generateSecretsMap(EncryptionScheme p) throws IOException {
        Map<String, String> secrets = new HashMap<>();
        if (p instanceof EncryptionSchemeV1 p1) {
            secrets.put(String.format(SECRETS_KEY, 1), base64Encoder.encodeToString(p1.getKeywordsMasterKey().getEncoded()));
            secrets.put(String.format(SECRETS_KEY, 2), base64Encoder.encodeToString(p1.getRNDKey().getEncoded()));
            secrets.put(String.format(SECRETS_KEY, 3), base64Encoder.encodeToString(p1.getDETKey().getEncoded()));
            secrets.put(SECRETS_IV, base64Encoder.encodeToString(p1.getIvDET()));
            secrets.put(SECRETS_SCHEMA_KEYWORD, p1.getSchemaKeyword());
        } else if (p instanceof EncryptionSchemeV2 p2) {
            secrets.put(String.format(SECRETS_KEY, 1), base64Encoder.encodeToString(p2.getKeywordsMasterKey().getEncoded()));
            secrets.put(String.format(SECRETS_KEY, 2), base64Encoder.encodeToString(p2.getRNDKey().getEncoded()));
            secrets.put(SECRETS_IV, base64Encoder.encodeToString(p2.getIvDET()));
            secrets.put(SECRETS_SCHEMA_KEYWORD, p2.getSchemaKeyword());
            secrets.put(SECRETS_LAST_EQ_TAG, base64Encoder.encodeToString(integerToByteArray(Math.toIntExact(p2.getLastEqTag()))));
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                 ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                oos.writeObject(new KeyPair(p2.getPubDGK(), p2.getPrivDGK()));
                secrets.put(SECRETS_KEY_PAIR, base64Encoder.encodeToString(bos.toByteArray()));
            }
        }
        return secrets;
    }

    public static Set<Triple> parseTriples(InputStream content, Lang lang) throws UnknownRDFLanguageException, InvalidNodeException {
        CollectorStreamTriples tripleCollector = new CollectorStreamTriples();
        RDFParser.source(content).lang(lang).parse(tripleCollector);
        return new HashSet<>(tripleCollector.getCollected());
    }

    public static Lang parseRDFLanguage(String syntax) throws UnknownRDFLanguageException {
        Lang l = RDFLanguages.nameToLang(syntax);
        if (l == null)
            throw new UnknownRDFLanguageException();
        return l;
    }

    public static String parseNode(Node node) throws InvalidNodeException {
        if (!node.isConcrete())
            throw new InvalidNodeException();
        if (node.isURI())
            return String.format(SIMPLE_NODE, node.getURI());
        else if (node.isLiteral())
            return String.format(LITERAL_NODE, node.getLiteralLexicalForm(), node.getLiteralDatatypeURI());
        else
            return String.format(BLANK_NODE, node.getBlankNodeId());
    }

    public static String parseKeyword(Node node) throws InvalidNodeException {
        if (!node.isConcrete())
            throw new InvalidNodeException();
        if (node.isURI())
            return String.format(SIMPLE_NODE, node.getURI());
        else if (node.isLiteral())
            return String.format(LITERAL_NODE, node.getLiteralLexicalForm(), node.getLiteralDatatypeURI());
        else
            return BLANK;
    }

    public static Node generateNode(String node) {
        String[] split = node.split(NODE_SEPARATOR);
        if (split[PREFIX_POS].equals(BLANK_PREFIX))
            return NodeFactory.createBlankNode(split[VALUE_POS]);
        else if (split[PREFIX_POS].equals(SIMPLE_PREFIX))
            return NodeFactory.createURI(split[VALUE_POS]);
        else
            return NodeFactory.createLiteral(
                    split[VALUE_POS],
                    TypeMapper.getInstance().getSafeTypeByName(split[LITERAL_DATATYPE_POS]));
    }

    public static byte[] DGKKeyToByteArray(DGKEqKey eqKey) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(eqKey);
            return bos.toByteArray();
        }
    }

    public static byte[] integerToByteArray(int integer) {
        return new byte[]{(byte) (integer >> 24), (byte) (integer >> 16), (byte) (integer >> 8), (byte) integer};
    }

    public static int byteArrayToInteger(byte[] iv) {
        return (iv[0] << 24) | ((iv[1] & 0xff) << 16) | ((iv[2] & 0xff) << 8) | (iv[3] & 0xff);
    }

    public static String generateKeyword(VariablesPattern pattern, String keyword1, String keyword2) {
        return String.format(KEYWORD_FORMAT, pattern, String.format(COMPOUND_KEYWORD, keyword1, keyword2));
    }

    public static String generateKeyword(VariablesPattern pattern, String keyword) {
        return String.format(KEYWORD_FORMAT, pattern, keyword);
    }

    public static Set<String> generateKeywords(Set<Triple> triples) throws InvalidNodeException {
        Set<String> keywords = new HashSet<>();
        String s, p, o;
        for (Triple t : triples) {
            s = parseKeyword(t.getSubject());
            p = parseKeyword(t.getPredicate());
            o = parseKeyword(t.getObject());
            keywords.add(generateKeyword(PO, s));
            keywords.add(generateKeyword(SO, p));
            keywords.add(generateKeyword(SP, o));
            keywords.add(generateKeyword(S, p, o));
            keywords.add(generateKeyword(P, s, o));
            keywords.add(generateKeyword(O, s, p));
        }
        return keywords;
    }

    public static String parseTriple(Node s, Node p, Node o) throws InvalidNodeException {
        String parsed_s, parsed_p, parsed_o;
        if (s.isVariable())
            parsed_s = ((Var) s).getVarName();
        else
            parsed_s = parseKeyword(s);

        if (p.isVariable())
            parsed_p = ((Var) p).getVarName();
        else
            parsed_p = parseKeyword(p);

        if (o.isVariable())
            parsed_o = ((Var) o).getVarName();
        else
            parsed_o = parseKeyword(o);
        return String.format(TRIPLE_KEYWORD, parsed_s, parsed_p, parsed_o);
    }

    public static String eqTagToString(BigInteger eqTag) {
        return base64Encoder.encodeToString(eqTag.toByteArray());
    }
    public static String eqTagBytesToString(byte[] eqTag) {
        return base64Encoder.encodeToString(eqTag);
    }

}
