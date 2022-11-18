package pt.fct.nova.id.srv.application.clients;


import org.apache.http.conn.ssl.SSLConnectionSocketFactory;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;


public class HTTPSClient {

    private static HttpClientBuilder httpClientBuilder;

    private static void createHttpClientBuilder() {
        httpClientBuilder = HttpClients.custom()
                .setSSLSocketFactory(SSLConnectionSocketFactory.getSystemSocketFactory());
    }

    public synchronized static CloseableHttpClient buildClient() {
        if (httpClientBuilder == null)
            createHttpClientBuilder();
        return httpClientBuilder.build();
    }

}
