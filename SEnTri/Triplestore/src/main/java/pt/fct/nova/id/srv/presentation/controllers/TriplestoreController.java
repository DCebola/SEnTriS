package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.jena.assembler.RuleSet;
import org.apache.jena.atlas.lib.NotImplemented;

import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Triple;
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.PrintUtil;
import org.apache.jena.vocabulary.RDF;
import pt.fct.nova.id.srv.application.clients.HTTPClient;
import pt.fct.nova.id.srv.application.clients.HTTPUtils;
import pt.fct.nova.id.srv.application.clients.IAMClient;
import pt.fct.nova.id.srv.application.query.execution.DefaultSPARQLExecution;
import pt.fct.nova.id.srv.application.query.execution.DefaultSPARQLWorker;
import pt.fct.nova.id.srv.application.query.execution.SPARQLExecution;
import pt.fct.nova.id.srv.application.query.plans.QueryExecutionPlan;
import pt.fct.nova.id.srv.application.storage.StorageEngine;
import pt.fct.nova.id.srv.application.storage.redis.RedisDefaultStorageEngine;
import pt.fct.nova.id.srv.presentation.api.TriplestoreAPI;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.stream.Collectors;

import static jakarta.ws.rs.core.Response.Status.*;
import static org.apache.jena.ontology.OntModelSpec.*;
import static pt.fct.nova.id.srv.application.clients.HTTPUtils.extractAccessToken;

@Path("")
public class TriplestoreController implements TriplestoreAPI {
    public static final String NO_ACCESS_TOKEN = "Malformed request: bearer token required.";
    public static final String INTERNAL_ERROR = "Internal error.";
    public static final String SUCCESSFUL_UPLOAD = "Successful upload.";
    public static final String SUCCESSFUL_DELETION = "Successful deletion.";
    public static final String NOT_IMPLEMENTED_ERROR = "Operation not yet supported.";
    private static final StorageEngine storageEngine = new RedisDefaultStorageEngine();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder();


    @Override
    public Response upload(String triplestoreID, byte[] triplesData, boolean isSchema, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.hasWriteAccess(httpClient, triplestoreID, accessToken)) {
            if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                return HTTPUtils.buildResponse(response);
            try (ByteArrayInputStream bis = new ByteArrayInputStream(triplesData);
                 ObjectInputStream ois = new ObjectInputStream(bis)) {
                Set<Triple> triples = (Set<Triple>) ois.readObject();
                if (isSchema)
                    storageEngine.saveSchema(triplestoreID, triples);
                else {
                    materializeDeductions(triplestoreID, triples);
                    storageEngine.save(triplestoreID, triples);
                }
                return Response.ok(SUCCESSFUL_UPLOAD).build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok(INTERNAL_ERROR).status(Status.INTERNAL_SERVER_ERROR).build();
        }

    }

    public Response answerSPARQLQuery(String triplestoreID, byte[] queryExecutionPlan, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.hasReadAccess(httpClient, triplestoreID, accessToken)) {
            if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                return HTTPUtils.buildResponse(response);

            try (ByteArrayInputStream bis = new ByteArrayInputStream(queryExecutionPlan);
                 ObjectInputStream ois = new ObjectInputStream(bis);
                 ByteArrayOutputStream bos = new ByteArrayOutputStream();
                 ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                QueryExecutionPlan executionPlan = (QueryExecutionPlan) ois.readObject();
                SPARQLExecution execution = new DefaultSPARQLExecution(executionPlan);
                execution.exec(new DefaultSPARQLWorker(triplestoreID, storageEngine));
                oos.writeObject(execution.getResults());
                return Response.ok(base64Encoder.encodeToString(bos.toByteArray())).build();
            }
        } catch (NotImplemented e) {
            return Response.ok(NOT_IMPLEMENTED_ERROR).status(NOT_IMPLEMENTED).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok(INTERNAL_ERROR).status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response delete(String triplestoreID, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();
        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.hasOwnerAccess(httpClient, triplestoreID, accessToken)) {
            if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                return HTTPUtils.buildResponse(response);
            storageEngine.delete(triplestoreID);
            return Response.ok(SUCCESSFUL_DELETION).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Status.INTERNAL_SERVER_ERROR).build();
        }

    }

    @Override
    public Response delete(String triplestoreID, byte[] triplesData, boolean isSchema, List<String> authorizationHeaders) {
        String accessToken = extractAccessToken(authorizationHeaders);
        if (accessToken == null)
            return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

        try (CloseableHttpClient httpClient = HTTPClient.buildClient();
             CloseableHttpResponse response = IAMClient.hasWriteAccess(httpClient, triplestoreID, accessToken)) {
            if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                return HTTPUtils.buildResponse(response);
            try (ByteArrayInputStream bis = new ByteArrayInputStream(triplesData);
                 ObjectInputStream ois = new ObjectInputStream(bis)) {
                Set<Triple> triples = (Set<Triple>) ois.readObject();
                if (isSchema) //TODO: Should have a config to turn on/off inference
                    storageEngine.deleteSchema(triplestoreID, triples);
                else {
                    materializeDeductions(triplestoreID, triples);
                    storageEngine.delete(triplestoreID, triples);
                }
                return Response.ok(SUCCESSFUL_DELETION).build();

            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok(INTERNAL_ERROR).status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private void materializeDeductions(String triplestoreID, Set<Triple> triples) {
        OntModel tbox = ModelFactory.createOntologyModel(OWL_MEM);
        OntModel abox = ModelFactory.createOntologyModel(OWL_MEM);
        GraphUtil.add(tbox.getGraph(), storageEngine.findSchema(triplestoreID).iterator());
        GraphUtil.add(abox.getGraph(), triples.iterator());
        OntModel ontology = ModelFactory.createOntologyModel(OWL_MEM_TRANS_INF, tbox);

        Map<Resource, Set<Statement>> groupedByResource = new HashMap<>();
        Set<Statement> stmts;
        for (Statement stmt : abox.listStatements().toSet()) {
            if (stmt.getSubject().isResource()) {
                stmts = groupedByResource.get(stmt.getSubject());
                if (stmts == null)
                    stmts = new HashSet<>();
                stmts.add(stmt);
                groupedByResource.put(stmt.getSubject(), stmts);
            }
        }
        Map<Resource, Set<OntClass>> equivalentClasses = new HashMap<>();
        Map<Property, OntClass> classRestrictions = new HashMap<>();
        Set<OntClass> s, s2;
        for (OntClass c : ontology.listClasses().toSet().stream().filter(RDFNode::isResource).collect(Collectors.toSet())) {
            System.out.println(PrintUtil.print(c));
            s = c.listSuperClasses().toSet();
            for (OntClass c2 : s)
                System.out.println(" -- " + PrintUtil.print(c2));
            s2 = c.listEquivalentClasses().toSet();
            for (OntClass c2 : s2)
                System.out.println(" -- " + PrintUtil.print(c2));
            s.addAll(s2);
            for (OntClass c2 : s) {
                if (c2.isRestriction()) {
                    if (c2.asRestriction().isSomeValuesFromRestriction()) {
                        System.out.println(" -r- " + PrintUtil.print(c2.asRestriction()) +
                                " | " + PrintUtil.print(c2.asRestriction().getOnProperty())
                                + " | " + PrintUtil.print(c2.asRestriction().asSomeValuesFromRestriction().getSomeValuesFrom()));
                        classRestrictions.put(c2.asRestriction().getOnProperty(), c2.asRestriction());
                    }
                    if (c2.asRestriction().isHasValueRestriction()) {
                        System.out.println(" -r- " + PrintUtil.print(c2.asRestriction()) +
                                " | " + PrintUtil.print(c2.asRestriction().getOnProperty())
                                + " | " + PrintUtil.print(c2.asRestriction().asHasValueRestriction().getHasValue()));
                        classRestrictions.put(c2.asRestriction().getOnProperty(), c2.asRestriction());
                    }
                }
            }
            equivalentClasses.put(c.asResource(), s);
        }

        Map<Resource, Set<? extends OntProperty>> equivalentProperties = new HashMap<>();
        Map<Resource, Set<? extends OntProperty>> inverseProperties = new HashMap<>();
        Set<Resource> symmetricProperties = new HashSet<>();
        Set<Resource> transitiveProperties = new HashSet<>();
        Set<? extends OntProperty> s3;
        for (OntProperty p : ontology.listOntProperties().toSet().stream().filter(RDFNode::isResource).collect(Collectors.toSet())) {
            System.out.println(PrintUtil.print(p));
            if (p.isInverseFunctionalProperty()) {
                inverseProperties.put(p.asResource(), p.listInverseOf().toSet());
            }
            if (p.isSymmetricProperty()) {
                symmetricProperties.add(p.asResource());
            }
            if (p.isTransitiveProperty()) {
                transitiveProperties.add(p.asResource());
            }
            s3 = p.listSuperProperties().toSet().stream().filter(RDFNode::isResource).collect(Collectors.toSet());
            for (OntProperty p2 : s3) {
                System.out.println(" + " + PrintUtil.print(p2));
                if (p2.isInverseFunctionalProperty()) {
                    inverseProperties.put(p2.asResource(), p.listInverseOf().toSet());
                }
                if (p2.isSymmetricProperty()) {
                    symmetricProperties.add(p.asResource());
                }
                if (p2.isTransitiveProperty()) {
                    transitiveProperties.add(p.asResource());
                }
            }
            equivalentProperties.put(p.asResource(), s3);
        }

        Property p;
        RDFNode o;
        OntClass c;
        for (Resource resource : groupedByResource.keySet()) {
            System.out.println("Resource: " + PrintUtil.print(resource));
            for (Statement statement : groupedByResource.get(resource)) {
                p = statement.getPredicate();
                o = statement.getObject();
                if (p.equals(RDF.type) && o.isResource()) {
                    System.out.println(" - " + PrintUtil.print(o));
                    s = equivalentClasses.get(o.asResource());
                    if (s != null) {
                        for (OntClass equivalentClass : s) {
                            System.out.println(" -- " + PrintUtil.print(equivalentClass));
                        }
                    }
                } else if (p.isResource()) {
                    System.out.println(" + " + PrintUtil.print(p));
                    c = classRestrictions.get(p);
                    if (c != null) {
                        if (c.asRestriction().isHasValueRestriction()) {
                            if (c.asRestriction().asHasValueRestriction().getHasValue().equals(o)) {
                                System.out.println(" +- " + PrintUtil.print(c));
                            }
                        }
                        if (c.asRestriction().isSomeValuesFromRestriction() && o.isResource()) {
                            s = equivalentClasses.get(c.asRestriction().asSomeValuesFromRestriction().getSomeValuesFrom());
                            for (OntClass c2 : s) {
                                if (c2.asResource().equals(o.asResource()))
                                    System.out.println(" +- " + PrintUtil.print(c));
                            }
                        }
                    }

                    s3 = equivalentProperties.get(p);
                    if (s3 != null) {
                        for (OntProperty equivalent : s3) {
                            System.out.println(" ++ " + PrintUtil.print(equivalent));
                        }
                    }
                    s3 = inverseProperties.get(p);
                    if (s3 != null) {
                        for (OntProperty inverse : s3) {
                            System.out.println(" +i+ " + PrintUtil.print(inverse));
                        }
                    }
                    if (symmetricProperties.contains(p)) {
                        System.out.println(" +s+ " + PrintUtil.print(s3));
                    }
                    if (transitiveProperties.contains(p)) {
                        System.out.println(" +t+ " + PrintUtil.print(s3));
                    }
                }
            }
        }
    }
}
