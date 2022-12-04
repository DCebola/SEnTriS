package pt.fct.nova.id.srv.application.crypto;

import org.bouncycastle.crypto.digests.SHA3Digest;
import org.bouncycastle.crypto.prng.DigestRandomGenerator;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

public final class PasswordUtils {
    private static final int s = Integer.parseInt(System.getenv("SALT_LENGTH")); //32
    private static final int k = Integer.parseInt(System.getenv("PASSWORD_HASH_LENGTH")); //256
    private static final int i = Integer.parseInt(System.getenv("PASSWORD_HASHING_ITERATIONS")); //10000
    private static final DigestRandomGenerator generator = new DigestRandomGenerator(new SHA3Digest(k));
    private static SecretKeyFactory keyFactory;

    public static byte[] hash(String password) throws NoSuchAlgorithmException, InvalidKeySpecException {
        return hash(password, generateRandomSalt());
    }

    public static boolean verify(String password, byte[] hash) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] salt = Arrays.copyOf(hash, s);
        return Arrays.equals(hash(password, salt), hash);
    }

    private static byte[] hash(String password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        initKeyFactory();
        KeySpec keyspec = new PBEKeySpec(password.toCharArray(), salt, i, k);
        return ByteBuffer.allocate(k + s)
                .put(salt)
                .put(keyFactory.generateSecret(keyspec).getEncoded())
                .array();
    }

    private static void initKeyFactory() throws NoSuchAlgorithmException {
        if (keyFactory == null)
            keyFactory = SecretKeyFactory.getInstance(System.getenv("PASSWORD_KEY_ALGORITHM"));
    }

    private static byte[] generateRandomSalt() {
        byte[] salt = new byte[s];
        generator.nextBytes(salt);
        return salt;
    }
}
