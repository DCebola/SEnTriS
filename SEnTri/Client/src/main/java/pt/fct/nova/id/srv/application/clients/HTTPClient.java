package pt.fct.nova.id.srv.application.clients;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;

import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;


public class HTTPClient {

    private static HttpClientBuilder httpClientBuilder;


    private static void createHttpClientBuilder() {
        httpClientBuilder = HttpClients.custom()
                .setConnectionManager(new PoolingHttpClientConnectionManager())
                .setSSLSocketFactory(SSLConnectionSocketFactory.getSystemSocketFactory());
    }

    public synchronized static HttpClient buildClient() {
        if (httpClientBuilder == null)
            createHttpClientBuilder();
        return httpClientBuilder.build();
    }
}
