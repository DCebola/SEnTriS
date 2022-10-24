package pt.fct.nova.id.srv.presentation.clients;


import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;

import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;

import javax.net.ssl.SSLSessionContext;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class HTTPSClient {

    private static SSLConnectionSocketFactory sslConnectionSocketFactory;
    private static HttpClientBuilder httpClientBuilder;


    private static void sslConnectionSocketFactory() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, KeyManagementException {
        SSLContextBuilder builder = new SSLContextBuilder();
        KeyStore ksTrust = KeyStore.getInstance(KeyStore.getDefaultType());
        ksTrust.load(new FileInputStream(System.getenv("DATA_OWNER_TRUSTSTORE_PATH")), System.getenv("DATA_OWNER_TRUSTSTORE_PWD").toCharArray());
        builder.loadTrustMaterial(ksTrust, new TrustSelfSignedStrategy());
        sslConnectionSocketFactory = new SSLConnectionSocketFactory(
                builder.build(),
                System.getenv("TLS_VERSION").split(","),
                System.getenv("TLS_CIPHER_SUITES").split(","),
                new NoopHostnameVerifier());



    }

    private static void createHttpClientBuilder() throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, KeyManagementException {
        if (sslConnectionSocketFactory == null)
            sslConnectionSocketFactory();
        httpClientBuilder = HttpClients.custom()
                .setSSLSocketFactory(sslConnectionSocketFactory);
    }


    public synchronized static CloseableHttpClient buildClient() throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, KeyManagementException {
        if (httpClientBuilder == null)
            createHttpClientBuilder();
        return httpClientBuilder.build();
    }

}
