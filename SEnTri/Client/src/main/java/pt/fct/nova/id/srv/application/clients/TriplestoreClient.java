package pt.fct.nova.id.srv.application.clients;

import jakarta.ws.rs.core.Cookie;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.jena.graph.Triple;
import pt.fct.nova.id.srv.application.protocols.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.query.plans.QueryExecutionPlan;
import pt.fct.nova.id.srv.application.query.plans.SimpleQueryExecutionPlan;
import pt.fct.nova.id.srv.presentation.controllers.ClientUtils;

import java.io.IOException;
import java.util.List;


public class TriplestoreClient {
    private static final String UPLOAD_URI = System.getenv("TRIPLESTORE_UPLOAD_URI");
    private static final String DELETE_ALL_URI = System.getenv("TRIPLESTORE_DELETE_ALL_URI");
    private static final String DELETE_SOME_URI = System.getenv("TRIPLESTORE_DELETE_SOME_URI");
    private static final String QUERY_URI = System.getenv("TRIPLESTORE_QUERY_URI");

    public static CloseableHttpResponse upload(Cookie cookie, String triplestoreID, List<Triple> triples, String accessToken) throws IOException, InvalidNodeException {
        return HttpUtils.sendPOSTRequest(cookie, String.format(UPLOAD_URI, triplestoreID), ClientUtils.triplesToHttpEntity(triples), accessToken);
    }

    public static CloseableHttpResponse query(Cookie cookie, String triplestoreID, SimpleQueryExecutionPlan plan, String accessToken) throws IOException {
        return HttpUtils.sendPOSTRequest(cookie, String.format(QUERY_URI, triplestoreID), ClientUtils.queryExecutionPlanToHttpEntity(plan), accessToken);
    }

    public static CloseableHttpResponse deleteAll(Cookie cookie, String triplestoreID, String accessToken) throws IOException {
        System.out.printf((DELETE_ALL_URI) + "%n", triplestoreID);
        return HttpUtils.sendDELETERequest(cookie, String.format(DELETE_ALL_URI, triplestoreID), accessToken);
    }

    public static CloseableHttpResponse deleteSome(Cookie cookie, String triplestoreID, List<Triple> triples, String accessToken) throws IOException, InvalidNodeException {
        return HttpUtils.sendPOSTRequest(cookie, String.format(DELETE_SOME_URI, triplestoreID), ClientUtils.triplesToHttpEntity(triples), accessToken);
    }


}
