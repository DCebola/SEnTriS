package pt.fct.nova.id.srv.application.crypto;

import com.goterl.lazysodium.LazySodiumJava;
import com.goterl.lazysodium.SodiumJava;
import com.goterl.lazysodium.interfaces.AEAD;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;
import pt.fct.nova.id.srv.presentation.controllers.ParsingUtils;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;

public class SymmetricEncryptionUtils {
    public static final String ALGORITHM = AEAD.Method.XCHACHA20_POLY1305_IETF.name();
    public static final int KEY_SIZE = AEAD.XCHACHA20POLY1305_IETF_KEYBYTES;
    public static final int TAG_SIZE = AEAD.XCHACHA20POLY1305_IETF_ABYTES;
    public static final int IV_SIZE = AEAD.XCHACHA20POLY1305_IETF_NPUBBYTES;
    private static final LazySodiumJava lazySodium = new LazySodiumJava(new SodiumJava(), StandardCharsets.UTF_8);

    public static byte[] encrypt(byte[] input, SecretKey key, byte[] iv) {
        byte[] ciphertext = new byte[input.length + TAG_SIZE];
        lazySodium.cryptoAeadXChaCha20Poly1305IetfEncrypt(ciphertext, null, input,
                input.length, new byte[0], 0L, null, iv, key.getEncoded());
        return ciphertext;
    }

    public static byte[] encrypt(byte[] input, SecretKey key) {
        byte[] ciphertext = new byte[input.length + TAG_SIZE];
        byte[] result = new byte[ciphertext.length + IV_SIZE];
        byte[] iv = generateRandomIV();
        lazySodium.cryptoAeadXChaCha20Poly1305IetfEncrypt(ciphertext, null, input, input.length, new byte[0], 0L, null, iv, key.getEncoded());
        System.arraycopy(iv, 0, result, 0, IV_SIZE);
        System.arraycopy(ciphertext, 0, result, IV_SIZE, ciphertext.length);
        return result;
    }

    public static byte[] decrypt(SecretKey key, byte[] input) throws AEADBadTagException {
        byte[] iv = new byte[IV_SIZE];
        byte[] ciphertext = new byte[input.length - IV_SIZE];
        System.arraycopy(input, 0, iv, 0, IV_SIZE);
        System.arraycopy(input, IV_SIZE, ciphertext, 0, ciphertext.length);
        return decrypt(key, ciphertext, iv);
    }

    public static byte[] decrypt(SecretKey key, byte[] ciphertext, byte[] iv) throws AEADBadTagException {
        byte[] plaintext = new byte[ciphertext.length - TAG_SIZE];
        if (!lazySodium.cryptoAeadXChaCha20Poly1305IetfDecrypt(plaintext, null, null, ciphertext, ciphertext.length, new byte[0], 0L, iv, key.getEncoded()))
            throw new AEADBadTagException();
        return plaintext;
    }

    public static SecretKey generateKey() {
        return new SecretKeySpec(generateRandomByteArray(KEY_SIZE), ALGORITHM);
    }

    public static SecretKey generateKey(SecretKey masterKey, byte[] info) {
        byte[] keyBytes = new byte[KEY_SIZE];
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA256Digest());
        hkdf.init(HKDFParameters.skipExtractParameters(masterKey.getEncoded(), info));
        hkdf.generateBytes(keyBytes, 0, KEY_SIZE);
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    public static SecretKey parseKey(byte[] keyBytes) {
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    public static byte[] generateRandomIV() {
        return generateRandomByteArray(IV_SIZE);
    }

    public static byte[] generateRandomByteArray(int size) {
        byte[] res = new byte[size];
        new SecureRandom().nextBytes(res);
        return res;
    }

    public static byte[] generateZeroFilledIV() {
        return new byte[IV_SIZE];
    }

    public static byte[] ivFromInteger(int integer) {
        byte[] iv = new byte[IV_SIZE];
        byte[] intBytes = ParsingUtils.integerToByteArray(integer);
        System.arraycopy(intBytes, 0, iv, IV_SIZE - Integer.BYTES, Integer.BYTES);
        return iv;
    }

}
