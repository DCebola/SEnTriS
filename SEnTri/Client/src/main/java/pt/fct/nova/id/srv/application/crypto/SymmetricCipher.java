package pt.fct.nova.id.srv.application.crypto;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

public class SymmetricCipher {
    private static final String ALGORITHM = System.getenv("SYMMETRIC_KEY_ALGORITHM");
    private static final String CIPHER_TRANSFORMATION = System.getenv("CIPHER_TRANSFORMATION");
    private static final int KEY_SIZE = Integer.parseInt(System.getenv("SYMMETRIC_KEY_SIZE"));
    private static final int IV_SIZE = Integer.parseInt(System.getenv("SYMMETRIC_IV_SIZE"));


    public static byte[] encrypt(byte[] input, SecretKey key, byte[] iv) throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] cipherText = cipher.doFinal(input);
        return ByteBuffer.allocate(cipherText.length + IV_SIZE)
                .put(iv)
                .put(cipherText)
                .array();
    }

    public static byte[] decrypt(SecretKey key, byte[] cipherText) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(cipherText, 0, IV_SIZE));
        return cipher.doFinal(Arrays.copyOfRange(cipherText, IV_SIZE, cipherText.length));
    }

    public static SecretKey generateKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        keyGenerator.init(KEY_SIZE);
        return keyGenerator.generateKey();
    }

    public static SecretKey generateKey(SecretKey masterKey, byte[] info) {
        int keyLength = KEY_SIZE / Byte.SIZE;
        byte[] keyBytes = new byte[keyLength];
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA256Digest());
        hkdf.init(HKDFParameters.skipExtractParameters(masterKey.getEncoded(), info));
        hkdf.generateBytes(keyBytes, 0, keyLength);
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    public static SecretKey parseKey(byte[] contents) {
        return new SecretKeySpec(contents, 0, contents.length, ALGORITHM);
    }

    public static byte[] generateRandomIV() {
        byte[] iv = new byte[IV_SIZE];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    public static void incrementIV(byte[] iv) {
        for (int i = iv.length - 1; i >= 0; --i) {
            iv[i] += 1;
            if (iv[i] != 0)
                break;
        }
    }

    public static byte[] generateZeroFilledIV() {
        return new byte[IV_SIZE];
    }
}
