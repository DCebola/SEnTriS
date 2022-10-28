package pt.fct.nova.id.srv.application.crypto;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class SymmetricCipher {
    private static final String ALGORITHM = System.getenv("SYMMETRIC_ALGORITHM");
    private static final String MODE = System.getenv("SYMMETRIC_MODE");
    private static final String PADDING = System.getenv("SYMMETRIC_PADDING");
    private static final int KEY_SIZE = Integer.parseInt(System.getenv("SYMMETRIC_KEY_SIZE"));
    private static final int IV_SIZE = Integer.parseInt(System.getenv("SYMMETRIC_IV_SIZE"));


    public byte[] encrypt(byte[] input, SecretKey key, byte[] iv) throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException {
        checkAlgorithm(ALGORITHM + "/" + MODE + "/" + PADDING);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] ciphertext = cipher.doFinal(input);
        return ByteBuffer.allocate(ciphertext.length + IV_SIZE)
                .put(ciphertext)
                .put(iv)
                .array();
    }

    private void checkAlgorithm(String alg) throws NoSuchAlgorithmException {
        if (!alg.equals("AES/GCM/NoPadding") && !alg.equals("ChaCha20-Poly1305/None/NoPadding"))
            throw new NoSuchAlgorithmException();
    }

    public static SecretKey generateKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        keyGenerator.init(KEY_SIZE);
        return keyGenerator.generateKey();
    }

    public static byte[] generateIv() {
        byte[] iv = new byte[IV_SIZE];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
}
