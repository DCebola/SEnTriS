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
import pt.fct.nova.id.srv.application.query.execution.DefaultSPARQLResult;
import pt.fct.nova.id.srv.application.query.execution.SPARQLResult;
import pt.fct.nova.id.srv.application.query.plans.DefaultQueryExecutionPlan;
import pt.fct.nova.id.srv.presentation.api.dtos.AuthForm;
import pt.fct.nova.id.srv.presentation.api.dtos.RequestDecisionForm;
import pt.fct.nova.id.srv.presentation.api.dtos.Role;
import pt.fct.nova.id.srv.presentation.exceptions.UnknownRDFLanguageException;

import javax.crypto.SecretKey;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static pt.fct.nova.id.srv.presentation.controllers.EncryptedTriplestoreV1Controller.*;

public class ParsingUtils {
    public static final String COOKIE_PARAM = "session";
    public static final String INTERNAL_ERROR = "Internal error.";
    public static final String BASIC_SEPARATOR = System.getenv("BASIC_SEPARATOR");
    public static final String IRI_SEPARATOR = System.getenv("IRI_SEPARATOR");
    private static final String BLANK_IRI_PREFIX = "B";
    private static final String SIMPLE_IRI_PREFIX = "S";
    private final static String BLANK_IRI = BLANK_IRI_PREFIX.concat(IRI_SEPARATOR).concat("%s");
    private static final String SIMPLE_IRI = SIMPLE_IRI_PREFIX.concat(IRI_SEPARATOR).concat("%s");
    private static final String LITERAL_IRI = "L".concat(IRI_SEPARATOR).concat("%s").concat(IRI_SEPARATOR).concat("%s");
    private static final int IRI_PREFIX_POS = 0;
    private static final int IRI_VALUE_POS = 1;
    private static final int LITERAL_IRI_DATATYPE_POS = 2;

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

    public static HttpEntity triplesListToHttpEntity(List<Triple> list) throws InvalidNodeException {
        return new StringEntity(gson.toJson(serializeTriples(list), List.class), ContentType.APPLICATION_JSON);
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

    public static List<String[]> serializeTriples(List<Triple> triples) throws InvalidNodeException {
        List<String[]> serialized = new ArrayList<>(triples.size());
        for (Triple t : triples)
            serialized.add(new String[]{
                    parseNodeIRI(t.getSubject()),
                    parseNodeIRI(t.getPredicate()),
                    parseNodeIRI(t.getObject()),
            });
        return serialized;
    }

    public static Lang parseRDFLanguage(String syntax) throws UnknownRDFLanguageException {
        Lang l = RDFLanguages.nameToLang(syntax);
        if (l == null)
            throw new UnknownRDFLanguageException();
        return l;
    }

    public static String parseNodeIRI(Node node) throws InvalidNodeException {
        if (!node.isConcrete())
            throw new InvalidNodeException();
        if (node.isURI())
            return String.format(SIMPLE_IRI, node.getURI());
        else if (node.isLiteral())
            return String.format(LITERAL_IRI, node.getLiteralLexicalForm(), node.getLiteralDatatypeURI());
        else
            return String.format(BLANK_IRI, node.getBlankNodeId());
    }

    public static String parseKeyword(Node node) throws InvalidNodeException {
        if (!node.isConcrete())
            throw new InvalidNodeException();
        if (node.isURI())
            return String.format(SIMPLE_IRI, node.getURI());
        else if (node.isLiteral())
            return String.format(LITERAL_IRI, node.getLiteralLexicalForm(), node.getLiteralDatatypeURI());
        else
            return BLANK;
    }

    public static Node generateNode(String iri) {
        String[] split_iri = iri.split(IRI_SEPARATOR);
        if (split_iri[IRI_PREFIX_POS].equals(BLANK_IRI_PREFIX))
            return NodeFactory.createBlankNode(split_iri[IRI_VALUE_POS]);
        else if (split_iri[IRI_PREFIX_POS].equals(SIMPLE_IRI_PREFIX))
            return NodeFactory.createURI(split_iri[IRI_VALUE_POS]);
        else
            return NodeFactory.createLiteral(
                    split_iri[IRI_VALUE_POS],
                    TypeMapper.getInstance().getSafeTypeByName(split_iri[LITERAL_IRI_DATATYPE_POS]));
    }


}
