package pt.fct.nova.id.srv.application.crypto;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

public class SymmetricCipher {
    private static final String ALGORITHM = System.getenv("SYMMETRIC_ALGORITHM");
    private static final String MODE = System.getenv("SYMMETRIC_MODE");
    private static final String PADDING = System.getenv("SYMMETRIC_PADDING");
    private static final int IV_SIZE = Integer.parseInt(System.getenv("SYMMETRIC_IV_SIZE"));

    public static String decrypt(SecretKey key, String encryptedNode) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {
        byte[] cipherText = encryptedNode.getBytes();
        Cipher cipher = Cipher.getInstance(ALGORITHM + "/" + MODE + "/" + PADDING);
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(Arrays.copyOf(cipherText, IV_SIZE)));
        return new String(cipher.doFinal(Base64.getDecoder().decode(cipherText)));
    }

}
