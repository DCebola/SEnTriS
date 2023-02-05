package pt.fct.nova.id.srv.application.crypto;

import com.goterl.lazysodium.LazySodiumJava;
import com.goterl.lazysodium.SodiumJava;
import com.goterl.lazysodium.interfaces.AEAD;

import javax.crypto.AEADBadTagException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class SymmetricEncryptionUtils {
    public static final String ALGORITHM = AEAD.Method.XCHACHA20_POLY1305_IETF.name();
    public static final int TAG_SIZE = AEAD.XCHACHA20POLY1305_IETF_ABYTES;
    public static final int IV_SIZE = AEAD.XCHACHA20POLY1305_IETF_NPUBBYTES;
    private static final LazySodiumJava lazySodium = new LazySodiumJava(new SodiumJava(), StandardCharsets.UTF_8);

    public static byte[] decrypt(SecretKey key, byte[] input) throws AEADBadTagException {
        byte[] iv = new byte[IV_SIZE];
        byte[] ciphertext = new byte[input.length - IV_SIZE];
        byte[] plaintext = new byte[ciphertext.length - TAG_SIZE];
        System.arraycopy(input, 0, iv, 0, IV_SIZE);
        System.arraycopy(input, IV_SIZE, ciphertext, 0, ciphertext.length);
        if (!lazySodium.cryptoAeadXChaCha20Poly1305IetfDecrypt(plaintext, null, null, ciphertext, ciphertext.length, new byte[0], 0L, iv, key.getEncoded()))
            throw new AEADBadTagException();
        return plaintext;
    }

    public static SecretKey parseKey(byte[] keyBytes) {
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }
}
