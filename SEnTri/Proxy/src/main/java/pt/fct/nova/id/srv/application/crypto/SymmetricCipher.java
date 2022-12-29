package pt.fct.nova.id.srv.application.crypto;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class SymmetricCipher {
    private static final String ALGORITHM = System.getenv("SYMMETRIC_KEY_ALGORITHM");
    private static final String CIPHER_TRANSFORMATION = System.getenv("CIPHER_TRANSFORMATION");
    private static final int IV_SIZE = Integer.parseInt(System.getenv("SYMMETRIC_IV_SIZE"));


    public static byte[] decrypt(SecretKey key, byte[] cipherText) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(cipherText, 0, IV_SIZE));
        return cipher.doFinal(Arrays.copyOfRange(cipherText, IV_SIZE, cipherText.length));
    }

    public static SecretKey parseKey(byte[] contents) {
        return new SecretKeySpec(contents, 0, contents.length, ALGORITHM);
    }
}
