package pt.fct.nova.id.srv.presentation.controllers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.lang.CollectorStreamTriples;
import pt.fct.nova.id.srv.application.protocols.EncryptionProtocol;
import pt.fct.nova.id.srv.application.protocols.Protocol1;
import pt.fct.nova.id.srv.application.protocols.ProtocolVersion;
import pt.fct.nova.id.srv.presentation.api.dtos.AccessForm;
import pt.fct.nova.id.srv.presentation.api.dtos.AuthForm;
import pt.fct.nova.id.srv.presentation.api.dtos.UploadForm;
import pt.fct.nova.id.srv.presentation.exceptions.MalformedSecretsException;
import pt.fct.nova.id.srv.presentation.exceptions.UnknownRDFLanguageException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static pt.fct.nova.id.srv.presentation.controllers.SecureTriplestoreController.*;

public class ClientUtils {

    public static final String COOKIE_PARAM = "session";
    public static final String COOKIE_LIFETIME = System.getenv("COOKIE_LIFETIME");
    public static final String INTERNAL_ERROR = "Internal error.";
    private static final Gson gson = new Gson();

    public static HttpEntity generateStoreForm(String username, String storeID) {
        List<NameValuePair> pairs = new ArrayList<>(2);
        pairs.add(new BasicNameValuePair("issuer", username));
        pairs.add(new BasicNameValuePair("store", storeID));
        return new UrlEncodedFormEntity(pairs, StandardCharsets.UTF_8);
    }

    public static HttpEntity generateSecretsForm(String storeID, Map<String, String> secrets) {
        return MultipartEntityBuilder
                .create()
                .addTextBody("storeID", storeID, ContentType.create(MediaType.TEXT_PLAIN))
                .addTextBody("secrets", gson.toJson(secrets), ContentType.create(MediaType.APPLICATION_JSON))
                .build();
    }

    public static HttpEntity credentialsFormToHttpEntity(AuthForm credentialsForm) {
        List<NameValuePair> pairs = new ArrayList<>(2);
        pairs.add(new BasicNameValuePair("username", credentialsForm.getUsername()));
        pairs.add(new BasicNameValuePair("password", credentialsForm.getPassword()));
        return new UrlEncodedFormEntity(pairs, StandardCharsets.UTF_8);
    }

    public static HttpEntity accessFormToHttpEntity(AccessForm accessForm) {
        List<NameValuePair> pairs = new ArrayList<>(2);
        pairs.add(new BasicNameValuePair("user", accessForm.getUser()));
        pairs.add(new BasicNameValuePair("write", String.valueOf(accessForm.getWrite())));
        return new UrlEncodedFormEntity(pairs, StandardCharsets.UTF_8);
    }

    public static HttpEntity generatePrivilegeRoleRequest(String issuer) {
        List<NameValuePair> pairs = new ArrayList<>(2);
        pairs.add(new BasicNameValuePair("issuer", issuer));
        pairs.add(new BasicNameValuePair("role", "PRIVILEGED"));
        return new UrlEncodedFormEntity(pairs, StandardCharsets.UTF_8);
    }

    public static HttpEntity objectToHttpEntity(Object obj) {
        return new StringEntity(gson.toJson(obj), StandardCharsets.UTF_8);
    }

    public static Protocol1 initProtocol1(String storeID, Map<String, String> secrets) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        SecretKey k1 = gson.fromJson(secrets.get(String.format(SECRETS_NTH_KEY, 1)), SecretKey.class);
        SecretKey k2 = gson.fromJson(secrets.get(String.format(SECRETS_NTH_KEY, 2)), SecretKey.class);
        SecretKey k3 = gson.fromJson(secrets.get(String.format(SECRETS_NTH_KEY, 3)), SecretKey.class);
        byte[] iv = decodeBase64(secrets.get(SECRETS_IV));
        return new Protocol1(storeID, k1, k2, k3, iv);
    }

    public static Map<String, String> generateSecretsMap(EncryptionProtocol p) {
        Map<String, String> secrets = new HashMap<>();
        if (p instanceof Protocol1 p1) {
            secrets.put(SECRETS_VERSION_KEY, ProtocolVersion.V1.toString());
            secrets.put(String.format(SECRETS_NTH_KEY, 1), gson.toJson(p1.getK1(), SecretKey.class));
            secrets.put(String.format(SECRETS_NTH_KEY, 2), gson.toJson(p1.getK2(), SecretKey.class));
            secrets.put(String.format(SECRETS_NTH_KEY, 3), gson.toJson(p1.getK3(), SecretKey.class));
            secrets.put(SECRETS_IV, Base64.encodeBase64URLSafeString(p1.getIv()));
        }
        return secrets;
    }

    public static Map<String, String> sanitizeSecrets(Map<String, String> secrets) throws MalformedSecretsException {
        Map<String, String> sanitizedSecrets = new HashMap<>();
        if (secrets == null)
            return sanitizedSecrets;
        ProtocolVersion version;
        try {
            version = ProtocolVersion.valueOf(secrets.get(SECRETS_VERSION_KEY));
        } catch (IllegalArgumentException e) {
            throw new MalformedSecretsException();
        }
        switch (version) {
            case V1 -> {
                String k1 = String.format(SECRETS_NTH_KEY, 1);
                String k2 = String.format(SECRETS_NTH_KEY, 2);
                String k3 = String.format(SECRETS_NTH_KEY, 3);
                sanitizedSecrets.put(SECRETS_VERSION_KEY, secrets.get(SECRETS_VERSION_KEY));
                putIfFound(sanitizedSecrets, k1, secrets.get(k1));
                putIfFound(sanitizedSecrets, k2, secrets.get(k2));
                putIfFound(sanitizedSecrets, k3, secrets.get(k3));
                putIfFound(sanitizedSecrets, SECRETS_IV, secrets.get(SECRETS_IV));
            }
            case V2 -> {
                //TODO: Implemented v2.
            }
        }

        return sanitizedSecrets;
    }

    private static void putIfFound(Map<String, String> sanitizedSecrets, String key, String val) throws MalformedSecretsException {
        if (val != null)
            sanitizedSecrets.put(key, val);
        else
            throw new MalformedSecretsException();
    }

    public static Map<String, String> parseSecrets(String secrets) {
        return gson.fromJson(secrets, new TypeToken<Map<String, String>>() {
        }.getType());
    }

    public static List<String> parseSearchResults(String results) {
        return gson.fromJson(results, new TypeToken<List<String>>() {
        }.getType());
    }

    public static List<Triple> parseTriples(InputStream content, Lang lang) throws UnknownRDFLanguageException {
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


}
