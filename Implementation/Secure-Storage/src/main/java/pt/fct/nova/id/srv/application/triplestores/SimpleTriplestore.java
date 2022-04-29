package pt.fct.nova.id.srv.application.triplestores;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import pt.fct.nova.id.srv.application.query.QueryEngine;
import pt.fct.nova.id.srv.application.storage.StorageEngine;

import java.util.Iterator;

public class SimpleTriplestore implements Triplestore {

    private final StorageEngine storageEngine;
    private final QueryEngine queryEngine;

    public SimpleTriplestore(StorageEngine storageEngine, QueryEngine queryEngine) {
        this.storageEngine = storageEngine;
        this.queryEngine = queryEngine;
    }

    @Override
    public boolean createDataset(String storeID, Iterator<Triple> triples) {
        boolean success = storageEngine.setupStore(storeID);
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
    public Model getDataset(String storeID) {
        Graph g = GraphFactory.createDefaultGraph();
        storageEngine.getTriples(storeID).forEach(g::add);
        Model m = ModelFactory.createModelForGraph(g);
        m.setNsPrefix("rdf", RDF.uri);
        m.setNsPrefix("rdfs", RDFS.uri);
        m.setNsPrefix("owl", OWL.NS);
        m.setNsPrefix("base", "http://swat.cse.lehigh.edu/onto/univ-bench.owl");
        return m;
    }


}
