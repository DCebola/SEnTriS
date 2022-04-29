package pt.fct.nova.id.srv.application.triplestores;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;

import java.util.Iterator;

public interface Triplestore {

    boolean createDataset(String storeID, Iterator<Triple> triples);

    Model getDataset(String storeID);
}
