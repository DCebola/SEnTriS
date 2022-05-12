package pt.fct.nova.id.srv.application.triplestores;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;

import java.util.Iterator;
import java.util.Map;

public interface Triplestore {

    boolean createDataset(String storeID, Iterator<Triple> triples, Map<String, String> namespaces);

    Model getDatasetModel(String storeID);

    QueryResult executeQuery(String query);
}
