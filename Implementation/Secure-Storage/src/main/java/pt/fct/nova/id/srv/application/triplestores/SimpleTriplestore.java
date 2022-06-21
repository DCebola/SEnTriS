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

import java.util.Iterator;
import java.util.Map;

public class SimpleTriplestore implements Triplestore {

    private final StorageEngine storageEngine;
    private final QueryEngine queryEngine;

    public SimpleTriplestore(StorageEngine storageEngine, QueryEngine queryEngine) {
        this.storageEngine = storageEngine;
        this.queryEngine = queryEngine;
    }

    @Override
    public boolean createDataset(String storeID, Iterator<Triple> triples, Map<String, String> namespaces) {
        boolean success = storageEngine.setupStore(storeID, namespaces);
        if (!success)
            return false;
        while (triples.hasNext()) {
            success = storageEngine.saveTriple(storeID, triples.next());
            if (!success) {
                storageEngine.deleteStore(storeID);
                return false;
            }
        }
        return true;
    }

    @Override
    public Model getDatasetModel(String storeID) {
        Graph g = GraphFactory.createDefaultGraph();
        storageEngine.getTriples(storeID).forEach(g::add);
        Model m = ModelFactory.createModelForGraph(g);
        storageEngine.getNamespaces(storeID).forEach(m::setNsPrefix);
        return m;
    }

    @Override
    public ResultSet executeQuery(String query) {
        QueryExecutionPlan plan = queryEngine.getQueryPlan(query);
        plan.getExecutionOrder().forEach(System.out::println);
        plan.getJobs().forEach((j, k) -> System.out.println("[" + j + " " + k + "]"));
        return new SimpleSPARQLExecution(plan).exec(storageEngine);
    }


}
