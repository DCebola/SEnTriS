package pt.fct.nova.id.srv.presentation.clients;


import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

public class HTTPSClient {
    private static SSLConnectionSocketFactory sslConnectionSocketFactory;
    private static HttpClientBuilder httpClientBuilder;

    private synchronized static void createSSLConnectionSocketFactory() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        SSLContextBuilder builder = new SSLContextBuilder();

        KeyStore ksTrust = KeyStore.getInstance(KeyStore.getDefaultType());
        builder.loadTrustMaterial(ksTrust, new TrustSelfSignedStrategy());
        sslConnectionSocketFactory = new SSLConnectionSocketFactory(
                builder.build(),
                System.getenv("TLS_VERSION").split(System.getenv(",")),
                System.getenv("TLS_CIPHER_SUITES").split(","),
                SSLConnectionSocketFactory.getDefaultHostnameVerifier());
    }

    private static void createHttpClientBuilder() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        if (sslConnectionSocketFactory == null)
            createSSLConnectionSocketFactory();
        httpClientBuilder = HttpClients.custom()
                .setSSLHostnameVerifier(new DefaultHostnameVerifier())
                .setSSLSocketFactory(sslConnectionSocketFactory);
    }


    public synchronized static CloseableHttpClient buildClient() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        if (httpClientBuilder == null)
            createHttpClientBuilder();
        return httpClientBuilder.build();

    }


}
