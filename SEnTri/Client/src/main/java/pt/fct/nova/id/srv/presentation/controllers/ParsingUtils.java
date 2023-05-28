package pt.fct.nova.id.srv.presentation.controllers;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.message.BasicNameValuePair;
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
import pt.fct.nova.id.srv.application.protocols.EncryptionProtocol;
import pt.fct.nova.id.srv.application.protocols.Protocol1;
import pt.fct.nova.id.srv.application.protocols.Protocol2;
import pt.fct.nova.id.srv.application.protocols.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.query.execution.DefaultSPARQLResult;
import pt.fct.nova.id.srv.application.query.execution.SPARQLResult;
import pt.fct.nova.id.srv.application.query.jobs.VariablesPattern;
import pt.fct.nova.id.srv.application.query.plans.DefaultQueryExecutionPlan;
import pt.fct.nova.id.srv.presentation.api.dtos.AuthForm;
import pt.fct.nova.id.srv.presentation.api.dtos.RequestDecisionForm;
import pt.fct.nova.id.srv.presentation.api.dtos.Role;
import pt.fct.nova.id.srv.presentation.exceptions.UnknownRDFLanguageException;

import javax.crypto.SecretKey;
import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.*;

import static pt.fct.nova.id.srv.application.protocols.EncryptionProtocol.*;
import static pt.fct.nova.id.srv.application.query.jobs.VariablesPattern.*;
import static pt.fct.nova.id.srv.presentation.controllers.EncryptedTriplestoreV1Controller.*;
import static pt.fct.nova.id.srv.presentation.controllers.EncryptedTriplestoreV2Controller.SECRETS_KEY_PAIR;
import static pt.fct.nova.id.srv.presentation.controllers.EncryptedTriplestoreV2Controller.SECRETS_LAST_EQ_TAG;

public class ParsingUtils {
    public static final String COOKIE_PARAM = "session";
    public static final String INTERNAL_ERROR = "Internal error.";
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

    public static HttpEntity generateTriplestoreForm(String username, String triplestoreID) {
        List<NameValuePair> pairs = new ArrayList<>(2);
        pairs.add(new BasicNameValuePair("issuer", username));
        pairs.add(new BasicNameValuePair("triplestoreID", triplestoreID));
        return new UrlEncodedFormEntity(pairs, StandardCharsets.UTF_8);
    }

    public static HttpEntity credentialsFormToHttpEntity(AuthForm credentialsForm) {
        List<NameValuePair> pairs = new ArrayList<>(2);
        pairs.add(new BasicNameValuePair("username", credentialsForm.getUsername()));
        pairs.add(new BasicNameValuePair("password", credentialsForm.getPassword()));
        return new UrlEncodedFormEntity(pairs, StandardCharsets.UTF_8);
    }

    public static UrlEncodedFormEntity requestDecisionFormToHttpEntity(RequestDecisionForm decisionForm) {
        List<NameValuePair> pairs = new ArrayList<>(2);
        pairs.add(new BasicNameValuePair("target", decisionForm.getTarget()));
        pairs.add(new BasicNameValuePair("accept", Boolean.toString(decisionForm.isAccept())));
        return new UrlEncodedFormEntity(pairs, StandardCharsets.UTF_8);
    }

    public static HttpEntity generateRoleRequest(String issuer, Role role) {
        List<NameValuePair> pairs = new ArrayList<>(2);
        pairs.add(new BasicNameValuePair("issuer", issuer));
        pairs.add(new BasicNameValuePair("role", role.name()));
        return new UrlEncodedFormEntity(pairs, StandardCharsets.UTF_8);
    }

    public static HttpEntity mapOfStringBytesToHttpEntity(Map<String, byte[]> map) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(map);
            return new ByteArrayEntity(bos.toByteArray(), ContentType.APPLICATION_OCTET_STREAM);
        }
    }

    public static HttpEntity mapOfBytesBytesToHttpEntity(Map<byte[], byte[]> map) throws IOException {
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

    public static HttpEntity bytesSetToHttpEntity(Set<byte[]> set) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(set);
            return new ByteArrayEntity(bos.toByteArray(), ContentType.APPLICATION_OCTET_STREAM);
        }
    }

    public static HttpEntity bytesListToHttpEntity(List<byte[]> list) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(list);
            return new ByteArrayEntity(bos.toByteArray(), ContentType.APPLICATION_OCTET_STREAM);
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

    public static HttpEntity generateV2SearchRequest(List<byte[]> trapdoors, BigInteger mask) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(trapdoors);
            return MultipartEntityBuilder.create()
                    .addBinaryBody("mask", mask.toByteArray())
                    .addBinaryBody("trapdoors", bos.toByteArray())
                    .build();
        }
    }

    public static Map<byte[], byte[]> parseSecretsMap(String secrets) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream is = new ByteArrayInputStream(base64Decoder.decode(secrets));
             ObjectInputStream ois = new ObjectInputStream(is)) {
            return (Map<byte[], byte[]>) ois.readObject();
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

    public static Protocol1 getProtocol1(Map<byte[], byte[]> secrets) {
        SecretKey k1 = SymmetricEncryptionUtils.parseKey(secrets.get(String.format(SECRETS_KEY, 1).getBytes(StandardCharsets.UTF_8)));
        SecretKey k2 = SymmetricEncryptionUtils.parseKey(secrets.get(String.format(SECRETS_KEY, 2).getBytes(StandardCharsets.UTF_8)));
        SecretKey k3 = SymmetricEncryptionUtils.parseKey(secrets.get(String.format(SECRETS_KEY, 3).getBytes(StandardCharsets.UTF_8)));
        byte[] iv = secrets.get(SECRETS_IV.getBytes(StandardCharsets.UTF_8));
        byte[] schemaKeyword = secrets.get(SECRETS_SCHEMA_KEYWORD.getBytes(StandardCharsets.UTF_8));
        return new Protocol1(k1, k2, k3, iv, schemaKeyword);
    }

    public static Protocol2 getProtocol2(Map<byte[], byte[]> secrets) throws IOException, ClassNotFoundException {
        SecretKey k1 = SymmetricEncryptionUtils.parseKey(secrets.get(String.format(SECRETS_KEY, 1).getBytes(StandardCharsets.UTF_8)));
        SecretKey k2 = SymmetricEncryptionUtils.parseKey(secrets.get(String.format(SECRETS_KEY, 2).getBytes(StandardCharsets.UTF_8)));
        KeyPair keyPair = DGKUtils.parseKeyPair(secrets.get(SECRETS_KEY_PAIR.getBytes(StandardCharsets.UTF_8)));
        byte[] iv = secrets.get(SECRETS_IV.getBytes(StandardCharsets.UTF_8));
        byte[] schemaKeyword = secrets.get(SECRETS_SCHEMA_KEYWORD.getBytes(StandardCharsets.UTF_8));
        long lastEqTag = byteArrayToInteger(secrets.get(SECRETS_LAST_EQ_TAG.getBytes(StandardCharsets.UTF_8)));
        System.out.println("Retrieved protocol 2.");
        return new Protocol2(k1, k2, keyPair, iv, schemaKeyword, lastEqTag);
    }

    public static Map<byte[], byte[]> generateSecretsMap(EncryptionProtocol p) throws IOException {
        Map<byte[], byte[]> secrets = new HashMap<>();
        if (p instanceof Protocol1 p1) {
            secrets.put(String.format(SECRETS_KEY, 1).getBytes(StandardCharsets.UTF_8), p1.getKeywordsMasterKey().getEncoded());
            secrets.put(String.format(SECRETS_KEY, 2).getBytes(StandardCharsets.UTF_8), p1.getRNDKey().getEncoded());
            secrets.put(String.format(SECRETS_KEY, 3).getBytes(StandardCharsets.UTF_8), p1.getDETKey().getEncoded());
            secrets.put(SECRETS_IV.getBytes(StandardCharsets.UTF_8), p1.getIvDET());
            secrets.put(SECRETS_SCHEMA_KEYWORD.getBytes(StandardCharsets.UTF_8), p1.getSchemaKeyword());
        } else if (p instanceof Protocol2 p2) {
            secrets.put(String.format(SECRETS_KEY, 1).getBytes(StandardCharsets.UTF_8), p2.getKeywordsMasterKey().getEncoded());
            secrets.put(String.format(SECRETS_KEY, 2).getBytes(StandardCharsets.UTF_8), p2.getRNDKey().getEncoded());
            secrets.put(SECRETS_IV.getBytes(StandardCharsets.UTF_8), p2.getIvDET());
            secrets.put(SECRETS_SCHEMA_KEYWORD.getBytes(StandardCharsets.UTF_8), p2.getSchemaKeyword());
            secrets.put(SECRETS_LAST_EQ_TAG.getBytes(StandardCharsets.UTF_8), integerToByteArray(Math.toIntExact(p2.getLastEqTag())));
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                 ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                oos.writeObject(new KeyPair(p2.getPubDGK(), p2.getPrivDGK()));
                secrets.put(SECRETS_KEY_PAIR.getBytes(StandardCharsets.UTF_8), bos.toByteArray());
            }
        }
        return secrets;
    }

    public static List<Triple> parseTriples(InputStream content, Lang lang) throws UnknownRDFLanguageException, InvalidNodeException {
        CollectorStreamTriples tripleCollector = new CollectorStreamTriples();
        RDFParser.source(content).lang(lang).parse(tripleCollector);
        return tripleCollector.getCollected();
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

    public static byte[] parseSearchID(String value) {
        return base64Decoder.decode(value);
    }

    public static BigInteger parseEqTag(String value) {
        return new BigInteger(value);
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

    public static Set<byte[]> generateKeywords(List<Triple> triples) throws InvalidNodeException {
        Set<byte[]> keywords = new HashSet<>();
        String s, p, o;
        for (Triple t : triples) {
            s = parseKeyword(t.getSubject());
            p = parseKeyword(t.getPredicate());
            o = parseKeyword(t.getObject());
            keywords.add(generateKeyword(PO, s).getBytes(StandardCharsets.UTF_8));
            keywords.add(generateKeyword(SO, p).getBytes(StandardCharsets.UTF_8));
            keywords.add(generateKeyword(SP, o).getBytes(StandardCharsets.UTF_8));
            keywords.add(generateKeyword(S, p, o).getBytes(StandardCharsets.UTF_8));
            keywords.add(generateKeyword(P, s, o).getBytes(StandardCharsets.UTF_8));
            keywords.add(generateKeyword(O, s, p).getBytes(StandardCharsets.UTF_8));
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


}
