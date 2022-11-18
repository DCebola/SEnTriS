package pt.fct.nova.id.srv.application.crypto;


import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.jcajce.JcaX500NameUtil;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

public class KeyStoreUtils {
    private static KeyStore keystore;

    public static void saveSecretKey(String alias, char[] password, SecretKey secretKey) throws KeyStoreException {
        initKeyStore();
        keystore.setEntry(alias, new KeyStore.SecretKeyEntry(secretKey), new KeyStore.PasswordProtection(password));
    }

    public static void saveKeyPair(String privateKeyAlias, char[] privateKeyPassword, String certificateAlias, KeyPair keyPair) throws KeyStoreException, UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, OperatorCreationException, CertIOException {
        initKeyStore();
        X509Certificate cert = generateCertificate(keyPair);
        keystore.setCertificateEntry(certificateAlias, cert);
        keystore.setKeyEntry(privateKeyAlias, keyPair.getPrivate(), privateKeyPassword, new Certificate[]{cert});
    }

    public static SecretKey getSecretKey(String alias, char[] password) throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException {
        initKeyStore();
        return (SecretKey) keystore.getKey(alias, password);
    }

    public static KeyPair getKeyPair(String privateKeyAlias, char[] privateKeyPassword, String certificateAlias) throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException {
        initKeyStore();
        X509Certificate cert = (X509Certificate) keystore.getCertificate(certificateAlias);
        return new KeyPair(cert.getPublicKey(), (PrivateKey) keystore.getKey(privateKeyAlias, privateKeyPassword));
    }

    private static void initKeyStore() throws KeyStoreException {
        if (keystore == null)
            keystore = KeyStore.getInstance(KeyStore.getDefaultType());
    }

    private static X509Certificate generateCertificate(KeyPair keyPair) throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException, OperatorCreationException, CertificateException, CertIOException {
        PrivateKey masterPrivateKey = (PrivateKey) keystore.getKey(System.getenv("MASTER_PRIVATE_KEY_ALIAS"),
                System.getenv("MASTER_PRIVATE_KEY_PASSWORD").toCharArray());
        X509Certificate cert = (X509Certificate) keystore.getCertificate(System.getenv("MASTER_CERTIFICATE_ALIAS"));
        return new JcaX509CertificateConverter().getCertificate(new JcaX509v3CertificateBuilder(
                JcaX500NameUtil.getIssuer(cert),
                new BigInteger(String.valueOf(new SecureRandom().nextLong())),
                new Date(),
                new Date(System.currentTimeMillis() + Long.parseLong(System.getenv("CERTIFICATE_VALIDITY"))),
                JcaX500NameUtil.getSubject(cert),
                keyPair.getPublic()
        ).addExtension(
                Extension.basicConstraints,
                true,
                new BasicConstraints(false)
        ).build(new JcaContentSignerBuilder(cert.getSigAlgName()).build(masterPrivateKey)));
    }
}
