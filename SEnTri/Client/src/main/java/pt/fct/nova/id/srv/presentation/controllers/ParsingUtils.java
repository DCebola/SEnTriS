package pt.fct.nova.id.srv.presentation.controllers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.lang.CollectorStreamTriples;
import pt.fct.nova.id.srv.application.crypto.SymmetricCipher;
import pt.fct.nova.id.srv.application.protocols.EncryptionProtocol;
import pt.fct.nova.id.srv.application.protocols.Protocol1;
import pt.fct.nova.id.srv.application.protocols.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.querying.execution.DefaultSPARQLResult;
import pt.fct.nova.id.srv.application.querying.execution.SPARQLResult;
import pt.fct.nova.id.srv.application.querying.jobs.VariablesPattern;
import pt.fct.nova.id.srv.application.querying.plans.DefaultQueryExecutionPlan;
import pt.fct.nova.id.srv.presentation.api.dtos.AuthForm;
import pt.fct.nova.id.srv.presentation.api.dtos.RequestDecisionForm;
import pt.fct.nova.id.srv.presentation.api.dtos.Role;
import pt.fct.nova.id.srv.presentation.exceptions.UnknownRDFLanguageException;

import javax.crypto.SecretKey;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static pt.fct.nova.id.srv.application.protocols.EncryptionProtocol.COMPOUND_KEYWORD;
import static pt.fct.nova.id.srv.application.protocols.EncryptionProtocol.KEYWORD_FORMAT;
import static pt.fct.nova.id.srv.application.querying.jobs.VariablesPattern.*;
import static pt.fct.nova.id.srv.presentation.controllers.EncryptedTriplestoreV1Controller.*;

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

    private static final Gson gson = new Gson();
    private static final String BLANK = "BLANK";
    private static final Base64.Decoder base64Decoder = Base64.getUrlDecoder();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder();

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

    public static HttpEntity mapOfStringStringToHttpEntity(Map<String, String> map) {
        return new StringEntity(gson.toJson(map, Map.class), ContentType.APPLICATION_JSON);
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

    public static HttpEntity stringSetToHttpEntity(Set<String> set) {
        return new StringEntity(gson.toJson(set, List.class), ContentType.APPLICATION_JSON);
    }

    public static HttpEntity stringListToHttpEntity(List<String> list) {
        return new StringEntity(gson.toJson(list, List.class), ContentType.APPLICATION_JSON);
    }

    public static HttpEntity queryExecutionPlanToHttpEntity(DefaultQueryExecutionPlan plan) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(plan);
            return new ByteArrayEntity(bos.toByteArray(), ContentType.APPLICATION_OCTET_STREAM);
        }
    }

    public static HttpEntity generateSecureQueryRequest(SecretKey key, DefaultQueryExecutionPlan plan) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(plan);
            return MultipartEntityBuilder.create()
                    .addBinaryBody("key", key.getEncoded())
                    .addBinaryBody("queryExecutionPlan", bos.toByteArray())
                    .build();
        }

    }

    public static Map<String, String> parseMapOfStringString(String secrets) {
        return gson.fromJson(secrets, new TypeToken<Map<String, String>>() {
        }.getType());
    }

    public static List<String> parseListOfStrings(String results) {
        return gson.fromJson(results, new TypeToken<List<String>>() {
        }.getType());
    }


    public static SPARQLResult parseSPARQLResult(String results) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream is = new ByteArrayInputStream(base64Decoder.decode(results));
             ObjectInputStream ois = new ObjectInputStream(is)) {
            return (DefaultSPARQLResult) ois.readObject();
        }
    }

    public static Protocol1 getProtocol1(Map<String, String> secrets) {
        SecretKey k1 = SymmetricCipher.parseKey(base64Decoder.decode(secrets.get(String.format(SECRETS_KEY, 1))));
        SecretKey k2 = SymmetricCipher.parseKey(base64Decoder.decode(secrets.get(String.format(SECRETS_KEY, 2))));
        SecretKey k3 = SymmetricCipher.parseKey(base64Decoder.decode(secrets.get(String.format(SECRETS_KEY, 3))));
        byte[] iv = base64Decoder.decode(secrets.get(SECRETS_IV));
        return new Protocol1(k1, k2, k3, iv);
    }

    public static Map<String, String> generateSecretsMap(EncryptionProtocol p) {
        Map<String, String> secrets = new HashMap<>();
        if (p instanceof Protocol1 p1) {
            secrets.put(String.format(SECRETS_KEY, 1), base64Encoder.encodeToString(p1.getKeywordsMasterKey().getEncoded()));
            secrets.put(String.format(SECRETS_KEY, 2), base64Encoder.encodeToString(p1.getRNDKey().getEncoded()));
            secrets.put(String.format(SECRETS_KEY, 3), base64Encoder.encodeToString(p1.getDETKey().getEncoded()));
            secrets.put(SECRETS_IV, base64Encoder.encodeToString(p1.getIvDET()));
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

    public static Set<String> generateKeywords(List<Triple> triples) throws InvalidNodeException {
        Set<String> keywords = new HashSet<>();
        String s, p, o;
        for (Triple t : triples) {
            s = parseKeyword(t.getSubject());
            p = parseKeyword(t.getPredicate());
            o = parseKeyword(t.getObject());
            keywords.add(ParsingUtils.generateKeyword(PO, s));
            keywords.add(ParsingUtils.generateKeyword(SO, p));
            keywords.add(ParsingUtils.generateKeyword(SP, o));
            keywords.add(ParsingUtils.generateKeyword(S, p, o));
            keywords.add(ParsingUtils.generateKeyword(P, s, o));
            keywords.add(ParsingUtils.generateKeyword(O, s, p));
        }
        return keywords;
    }
}
