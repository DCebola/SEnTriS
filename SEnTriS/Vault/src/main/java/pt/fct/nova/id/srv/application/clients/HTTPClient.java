package pt.fct.nova.id.srv.application.clients;


import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.ssl.SSLContexts;


public class HTTPClient {
    private static HttpClientBuilder httpClientBuilder;

    private static void createHttpClientBuilder() {
        PoolingHttpClientConnectionManager connPool = PoolingHttpClientConnectionManagerBuilder.create()
                .setTlsSocketStrategy((TlsSocketStrategy) ClientTlsStrategyBuilder.create()
                        .setSslContext(SSLContexts.createSystemDefault())
                        .setTlsVersions(TLS.V_1_3)).build();
        connPool.setMaxTotal(Integer.parseInt(System.getenv("CLIENT_CONN_POOL_MAX_TOTAL")));
        connPool.setDefaultMaxPerRoute(Integer.parseInt(System.getenv("CLIENT_CONN_POOL_MAX_PER_ROUTE")));
        httpClientBuilder = HttpClients.custom()
                .setConnectionManager(connPool)
                .setConnectionManagerShared(true);
    }

    public synchronized static CloseableHttpClient buildClient() {
        if (httpClientBuilder == null)
            createHttpClientBuilder();
        return httpClientBuilder.build();
    }
}
