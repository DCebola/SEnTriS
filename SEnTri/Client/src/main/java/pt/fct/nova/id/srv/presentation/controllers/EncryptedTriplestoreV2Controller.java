package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.query.SortCondition;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ResultSetStream;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingBuilder;
import org.apache.jena.sparql.engine.binding.BindingComparator;
import pt.fct.nova.id.srv.application.SPARQLQueryEngine;
import pt.fct.nova.id.srv.application.clients.*;
import pt.fct.nova.id.srv.application.protocols.Protocol1;
import pt.fct.nova.id.srv.application.protocols.Utils;
import pt.fct.nova.id.srv.application.protocols.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.query.execution.SPARQLResult;
import pt.fct.nova.id.srv.application.query.jobs.Job;
import pt.fct.nova.id.srv.application.query.jobs.SecureSearchJob;
import pt.fct.nova.id.srv.application.query.jobs.SerializableBinding;
import pt.fct.nova.id.srv.application.query.jobs.SerializableSortCondition;
import pt.fct.nova.id.srv.application.query.plans.DefaultQueryExecutionPlan;
import pt.fct.nova.id.srv.application.query.plans.QueryExecutionPlan;
import pt.fct.nova.id.srv.application.query.plans.SecureSPARQLPlanner;
import pt.fct.nova.id.srv.presentation.api.EncryptedTriplestoreAPI;
import pt.fct.nova.id.srv.presentation.api.dtos.QueryForm;
import pt.fct.nova.id.srv.presentation.api.dtos.UploadForm;
import pt.fct.nova.id.srv.presentation.exceptions.UnknownRDFLanguageException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.OK;
import static pt.fct.nova.id.srv.presentation.controllers.EncryptedTriplestoreV1Controller.*;
import static pt.fct.nova.id.srv.presentation.controllers.ParsingUtils.*;
import static pt.fct.nova.id.srv.presentation.controllers.TriplestoreController.*;

@Path("triplestores/v2/encrypted")
public class EncryptedTriplestoreV2Controller extends EncryptedTriplestoreController implements EncryptedTriplestoreAPI{

    @Override
    public Response create(Cookie cookie, UploadForm form) {
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            String triplestoreID = form.getTriplestoreID();
            String issuer = form.getIssuer();
            HTTPResponse response = createTriplestoreAccessPolicy(httpClient, cookie, triplestoreID, issuer);
            if (response.getStatus() != OK)
                return response.build();

            response = createAccessToken(httpClient, cookie, issuer, triplestoreID);
            if (response.getStatus() != OK)
                return response.build();
            String accessToken = response.getBody();

            if (form.getContents() == null)
                return Response.ok(SUCCESSFUL_CREATION).build();
            List<Triple> triples = parseTriples(form.getContents(), parseRDFLanguage(form.getSyntax()));

            response = acquireTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
            if (response.getStatus() != OK) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }

            Protocol1 p = new Protocol1();
            response = saveProtocolSecrets(httpClient, triplestoreID, generateSecretsMap(p), accessToken);
            if (response.getStatus() != OK) {
                HTTPResponse response2 = deleteTriplestoreAccessPolicy(httpClient, cookie, triplestoreID, accessToken);
                if (response2.getStatus() != OK)
                    return response2.build();
                releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }

            Collections.shuffle(triples);
            p.exec(triples);

            response = uploadEncryptedTriplestoreContents(httpClient, triplestoreID, p.getEncryptedT(), accessToken);
            releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return response.build();
        } catch (UnknownRDFLanguageException e) {
            e.printStackTrace();
            return Response.ok(INVALID_SYNTAX).status(Response.Status.BAD_REQUEST).build();
        } catch (InvalidNodeException e) {
            e.printStackTrace();
            return Response.ok(BAD_NODE).status(Response.Status.BAD_REQUEST).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response delete(Cookie cookie, String triplestoreID, String issuer) {
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            HTTPResponse response = createAccessToken(httpClient, cookie, issuer, triplestoreID);
            if (response.getStatus() != OK)
                return response.build();
            String accessToken = response.getBody();

            response = acquireTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
            if (response.getStatus() != OK) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }

            response = deleteEncryptedTriplestoreContents(httpClient, triplestoreID, accessToken);
            if (response.getStatus() != OK) {
                releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            }
            response = deleteProtocolSecrets(httpClient, triplestoreID, accessToken);
            if (response.getStatus() != OK) {
                releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            }
            response = deleteTriplestoreAccessPolicy(httpClient, cookie, triplestoreID, accessToken);
            if (response.getStatus() != OK) {
                releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }
            return Response.ok(SUCCESSFUL_DELETION).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response upload(Cookie cookie, UploadForm form) {
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            String issuer = form.getIssuer();
            String triplestoreID = form.getTriplestoreID();

            HTTPResponse response = createAccessToken(httpClient, cookie, issuer, triplestoreID);
            if (response.getStatus() != OK)
                return response.build();
            String accessToken = response.getBody();

            response = acquireTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
            if (response.getStatus() != OK) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }

            if (form.getContents() == null)
                return Response.ok(EMPTY_UPLOAD).status(Response.Status.BAD_REQUEST).build();

            List<Triple> triples = parseTriples(form.getContents(), parseRDFLanguage(form.getSyntax()));
            if (!triples.isEmpty())
                return Response.ok(EMPTY_UPLOAD).status(Response.Status.BAD_REQUEST).build();

            response = getProtocolSecrets(httpClient, triplestoreID, accessToken);
            if (response.getStatus() != OK) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }
            Map<String, String> secrets = ParsingUtils.parseMapOfStringString(response.getBody());

            Protocol1 protocol = initProtocol1(secrets);
            Collections.shuffle(triples);

            response = fetchAndUpdateKeywords(httpClient, triplestoreID, protocol, protocol.generateKeywordTrapdoorMap(triples).entrySet(), accessToken);
            if (response != null && response.getStatus() != OK) {
                releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            }

            Collections.shuffle(triples);
            protocol.exec(triples);

            response = uploadEncryptedTriplestoreContents(httpClient, triplestoreID, protocol.getEncryptedT(), accessToken);
            releaseTriplestoreLock(httpClient, cookie, triplestoreID, accessToken);
            deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
            return response.build();
        } catch (UnknownRDFLanguageException e) {
            return Response.ok(INVALID_SYNTAX).status(Response.Status.BAD_REQUEST).build();
        } catch (InvalidNodeException e) {
            return Response.ok(BAD_NODE).status(Response.Status.BAD_REQUEST).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response answerSPARQLQuery(Cookie cookie, QueryForm form) {
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            String triplestoreID = form.getTriplestoreID();
            String issuer = form.getIssuer();

            HTTPResponse response = createAccessToken(httpClient, cookie, issuer, triplestoreID);
            if (response.getStatus() != OK)
                return response.build();
            String accessToken = response.getBody();

            response = getProtocolSecrets(httpClient, triplestoreID, accessToken);
            if (response.getStatus() != OK) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }
            Map<String, String> secrets = ParsingUtils.parseMapOfStringString(response.getBody());

            Protocol1 protocol = initProtocol1(secrets);
            SecureSPARQLPlanner planner = new SecureSPARQLPlanner(protocol);
            DefaultQueryExecutionPlan plan = (DefaultQueryExecutionPlan) new SPARQLQueryEngine(planner).getQueryPlan(form.getQuery());

            Set<String> keywords = planner.getKeywords();
            List<String> keywordList = new ArrayList<>(keywords.size());
            List<String> trapdoors = new ArrayList<>(keywords.size());
            for (String keyword : keywords) {
                keywordList.add(keyword);
                trapdoors.add(protocol.generateTrapdoor(keyword));
            }
            response = searchEncryptedTriplestoreContents(httpClient, triplestoreID, trapdoors, accessToken);
            if (response.getStatus() != OK)
                return response.build();
            Map<Var, Var> obfuscationMap = planner.getObfuscationMap();
            List<String> encryptedKeywordsInfo = ParsingUtils.parseListOfStrings(response.getBody());
            Map<String, Integer> keywordsInfo = new HashMap<>(encryptedKeywordsInfo.size());
            String info;
            for (int i = 0; i < encryptedKeywordsInfo.size(); i++) {
                info = encryptedKeywordsInfo.get(i);
                if (info != null) {
                    int total = Utils.integerFromByteArray(protocol.decryptRNDLayer(info));
                    keywordsInfo.put(keywordList.get(i), total);
                } else
                    return getEmptySPARQLQueryResult(plan, obfuscationMap);
            }

            response = prepareSearches(httpClient, protocol, triplestoreID, plan, planner.getSearchJobsIDs(), keywordsInfo, accessToken);
            if (response != null && response.getStatus() != OK) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }

            response = query(httpClient, protocol.getK2(), plan, accessToken);
            if (response.getStatus() != OK) {
                deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return response.build();
            }
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                List<Var> vars = new LinkedList<>();
                for (Var var : plan.getVars())
                    vars.add(obfuscationMap.get(var));
                SPARQLResult sparqlResult = ParsingUtils.parseSPARQLResult(response.getBody());
                Collection<Binding> bindings = decryptBindings(sparqlResult.getBindings(), obfuscationMap, protocol);
                bindings = orderResultsIfNeeded(
                        sparqlResult.isOrdered(),
                        sparqlResult.isDistinct(),
                        sparqlResult.getSortConditions(),
                        bindings);
                ResultSetFormatter.outputAsJSON(out, ResultSetStream.create(vars, bindings.iterator()));
                CloseableHttpResponse ignored = IAMClient.deleteAccessToken(httpClient, cookie, triplestoreID, accessToken);
                return Response.ok(out.toByteArray()).build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    private HTTPResponse prepareSearches(CloseableHttpClient httpClient, Protocol1 protocol,
                                         String triplestoreID, QueryExecutionPlan plan, Set<String> searchJobsIDs,
                                         Map<String, Integer> keywordsInfo, String accessToken) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, IOException {
        HTTPResponse response;
        Map<String, String> preparedSearches = new HashMap<>();
        String keyword, searchID;
        Var var;
        SecureSearchJob secureSearchJob;
        Map<String, Job> jobs = plan.getJobs();
        List<String> trapdoors;
        for (String id : plan.getExecutionOrder()) {
            System.out.println(plan.getJobs().get(id));
        }
        for (String jobID : searchJobsIDs) {
            secureSearchJob = (SecureSearchJob) jobs.get(jobID);
            System.out.println("Preparing: " + jobID);
            for (Map.Entry<Var, String> entry : secureSearchJob.getSearches().entrySet()) {
                var = entry.getKey();
                keyword = entry.getValue();
                searchID = preparedSearches.get(keyword);
                if (searchID == null) {
                    trapdoors = protocol.generateTrapdoors(keyword, keywordsInfo.get(keyword));
                    Collections.shuffle(trapdoors);
                    response = prepareSearch(httpClient, triplestoreID, trapdoors, accessToken);
                    if (response.getStatus() != OK) {
                        return response;
                    }
                    searchID = response.getBody();
                    preparedSearches.put(keyword, searchID);
                }
                System.out.println(var + "->" + searchID);
                secureSearchJob.prepareSearch(var, searchID);
            }
        }
        return null;
    }

    private Collection<Binding> decryptBindings(Collection<SerializableBinding> bindings, Map<Var, Var> obfuscationMap, Protocol1 protocol) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        Collection<Binding> decryptedBindings = new LinkedList<>();
        BindingBuilder builder = Binding.builder();
        for (SerializableBinding binding : bindings) {
            for (Iterator<Var> it = binding.vars(); it.hasNext(); ) {
                Var var = it.next();
                builder.add(obfuscationMap.get(var), generateNode(new String(protocol.decryptDETLayer(binding.get(var)))));
            }
            decryptedBindings.add(builder.build());
            builder.reset();
        }
        return decryptedBindings;
    }
}
