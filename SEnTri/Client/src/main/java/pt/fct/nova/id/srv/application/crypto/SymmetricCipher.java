package pt.fct.nova.id.srv.application.crypto;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
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

    public static byte[] encrypt(byte[] input, SecretKey key) throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException {
        return encrypt(input, key, generateIV());
    }

    public static SecretKey generateKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        keyGenerator.init(KEY_SIZE);
        return keyGenerator.generateKey();
    }

    public static SecretKey parseKey(byte[] contents) {
        return new SecretKeySpec(contents, 0, contents.length, ALGORITHM);
    }

    public static byte[] generateIV() {
        byte[] iv = new byte[IV_SIZE];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    public static byte[] incrementIV(byte[] iv) {
        BigInteger ivAsBigInt = new BigInteger(iv);
        BigInteger plusOne = ivAsBigInt.add(BigInteger.ONE);
        System.out.println(ivAsBigInt);
        return plusOne.toByteArray();
    }
}
