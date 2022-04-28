package pt.fct.nova.id.srv.application.triplestores;

import org.apache.jena.graph.Triple;

import java.util.Iterator;

public interface Triplestore {

    boolean createDataset(String storeID, Iterator<Triple> triples);

    Iterator<Triple> getDataset(String storeID);
}
