package pt.fct.nova.id.srv.application.clients;


import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;


public class HTTPClient {
    private static HttpClientBuilder httpClientBuilder;

    private static void createHttpClientBuilder() {
        PoolingHttpClientConnectionManager connPool = new PoolingHttpClientConnectionManager();
        connPool.setMaxTotal(Integer.parseInt(System.getenv("CLIENT_CONN_POOL_MAX_TOTAL")));
        connPool.setDefaultMaxPerRoute(Integer.parseInt(System.getenv("CLIENT_CONN_POOL_MAX_PER_ROUTE")));
        httpClientBuilder = HttpClients.custom()
                .setConnectionManager(connPool)
                .setConnectionManagerShared(true)
                .setSSLSocketFactory(SSLConnectionSocketFactory.getSystemSocketFactory());
    }

    public synchronized static CloseableHttpClient buildClient() {
        if (httpClientBuilder == null)
            createHttpClientBuilder();
        return httpClientBuilder.build();
    }

}
