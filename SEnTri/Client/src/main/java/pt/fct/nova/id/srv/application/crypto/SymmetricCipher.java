package pt.fct.nova.id.srv.application.crypto;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public class SymmetricCipher {
    private static final String ALGORITHM = System.getenv("SYMMETRIC_ALGORITHM");
    private static final String MODE = System.getenv("SYMMETRIC_MODE");
    private static final String PADDING = System.getenv("SYMMETRIC_PADDING");
    private static final int KEY_SIZE = Integer.parseInt(System.getenv("SYMMETRIC_KEY_SIZE"));
    private static final int IV_SIZE = Integer.parseInt(System.getenv("SYMMETRIC_IV_SIZE"));


    public static byte[] encrypt(byte[] input, SecretKey key, byte[] iv) throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance(ALGORITHM + "/" + MODE + "/" + PADDING);
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] cipherText = cipher.doFinal(input);
        return ByteBuffer.allocate(cipherText.length + IV_SIZE)
                .put(cipherText)
                .put(iv)
                .array();
    }

    public static byte[] decrypt(SecretKey key, byte[] cipherText) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance(ALGORITHM + "/" + MODE + "/" + PADDING);
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(Arrays.copyOfRange(cipherText, cipherText.length - IV_SIZE, cipherText.length)));
        return cipher.doFinal(Base64.getDecoder().decode(cipherText));
    }

    public static byte[] encrypt(byte[] input, SecretKey key) throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException {
        return encrypt(input, key, generateIV());
    }

    public static SecretKey generateKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM + "/" + MODE + "/" + PADDING);
        keyGenerator.init(KEY_SIZE);
        return keyGenerator.generateKey();
    }

    public static byte[] generateIV() {
        byte[] iv = new byte[IV_SIZE];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    public static byte[] incrementIV(byte[] iv) {
        BigInteger ivAsBigInt = new BigInteger(iv);
        return ivAsBigInt.add(BigInteger.ONE).toByteArray();
    }


}
