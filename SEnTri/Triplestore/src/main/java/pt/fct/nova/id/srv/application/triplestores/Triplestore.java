package pt.fct.nova.id.srv.application.triplestores;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.graph.GraphFactory;
import pt.fct.nova.id.srv.application.query.QueryEngine;
import pt.fct.nova.id.srv.application.query.execution.SimpleSPARQLExecution;
import pt.fct.nova.id.srv.application.query.plans.QueryExecutionPlan;
import pt.fct.nova.id.srv.application.storage.StorageEngine;
import pt.fct.nova.id.srv.application.storage.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.storage.exceptions.StoreAlreadyExistsException;
import pt.fct.nova.id.srv.application.storage.exceptions.StoreNotFoundException;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
public class Triplestore {

    private final StorageEngine storageEngine;
    private final QueryEngine queryEngine;

    public Triplestore(StorageEngine storageEngine, QueryEngine queryEngine) {
        this.storageEngine = storageEngine;
        this.queryEngine = queryEngine;
    }

    public void createDataset(String storeID, List<Triple> triples, Map<String, String> namespaces) throws StoreAlreadyExistsException, InvalidNodeException {
        verifyStoreDoesNotExist(storeID);
        boolean success = storageEngine.setupStore(storeID, namespaces);
        if (!success) {
            storageEngine.deleteStore(storeID);
            throw new RuntimeException();
        } else if (triples != null)
            addTriples(storeID, triples);
    }

    private void addTriples(String storeID, List<Triple> triples) throws InvalidNodeException {
        boolean success;
        if (triples != null) {
            for (Triple t: triples) {
                success = storageEngine.saveTriple(storeID, t);
                if (!success) {
                    storageEngine.deleteStore(storeID);
                    throw new RuntimeException();
                }
            }
        }
    }

    public void uploadData(String storeID, List<Triple> triples, Map<String, String> namespaces) throws StoreNotFoundException, InvalidNodeException {
        verifyStoreExists(storeID);
        if (namespaces != null)
            storageEngine.saveNamespaces(storeID, namespaces);
        addTriples(storeID, triples);
    }

    public Model getDatasetModel(String storeID) throws StoreNotFoundException {
        verifyStoreExists(storeID);
        Graph g = GraphFactory.createDefaultGraph();
        storageEngine.getTriples(storeID).forEach(g::add);
        Model m = ModelFactory.createModelForGraph(g);
        storageEngine.getNamespaces(storeID).forEach(m::setNsPrefix);
        return m;
    }


    public ResultSet executeQuery(String storeID, String query) throws StoreNotFoundException {
        verifyStoreExists(storeID);
        QueryExecutionPlan plan = queryEngine.getQueryPlan(query);
        return new SimpleSPARQLExecution(plan).exec(storeID, storageEngine);
    }


    private void verifyStoreExists(String storeID) throws StoreNotFoundException {
        try {
            storageEngine.checkID(storeID);
        } catch (StoreAlreadyExistsException ignored) {
        }
    }

    private void verifyStoreDoesNotExist(String storeID) throws StoreAlreadyExistsException {
        try {
            storageEngine.checkID(storeID);
        } catch (StoreNotFoundException ignored) {
        }
    }

}
