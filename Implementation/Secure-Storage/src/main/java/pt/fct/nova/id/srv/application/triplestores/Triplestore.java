package pt.fct.nova.id.srv.application.triplestores;

import org.apache.jena.graph.Triple;

import java.util.Iterator;

public interface Triplestore {
    static final String BLANK_URI = "_";

    void createDataset(String storeID, Iterator<Triple> triples);

    Iterator<Triple> getDataset(String storeID);
}
