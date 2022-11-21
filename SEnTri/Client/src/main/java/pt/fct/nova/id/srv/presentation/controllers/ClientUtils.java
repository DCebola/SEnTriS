package pt.fct.nova.id.srv.presentation.controllers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import pt.fct.nova.id.srv.application.protocols.EncryptionProtocol;
import pt.fct.nova.id.srv.application.protocols.Protocol1;
import pt.fct.nova.id.srv.application.protocols.ProtocolVersion;
import pt.fct.nova.id.srv.presentation.api.dtos.UploadForm;
import pt.fct.nova.id.srv.presentation.exceptions.MalformedSecretsException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static pt.fct.nova.id.srv.presentation.controllers.SecureTriplestoreController.*;

public class ClientUtils {
    private static final Gson gson = new Gson();

    public static HttpEntity uploadFormToHttpEntity(UploadForm form) {
        return MultipartEntityBuilder
                .create()
                .addTextBody("syntax", form.getSyntax(), ContentType.create(MediaType.TEXT_PLAIN))
                .addTextBody("namespaces", gson.toJson(form.getNamespaces()), ContentType.create(MediaType.APPLICATION_JSON))
                .addBinaryBody("contents", form.getContents())
                .build();
    }

    public static HttpEntity secretsToHttpEntity(String issuer, String storeID, Map<String, String> secrets) {
        return MultipartEntityBuilder
                .create()
                .addTextBody("issuer", issuer, ContentType.create(MediaType.TEXT_PLAIN))
                .addTextBody("storeID", storeID, ContentType.create(MediaType.TEXT_PLAIN))
                .addTextBody("secrets", gson.toJson(secrets), ContentType.create(MediaType.APPLICATION_JSON))
                .build();
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

    public static List<String> parseSearchResults(String results) {
        return gson.fromJson(results, new TypeToken<List<String>>(){}.getType());
    }
}
