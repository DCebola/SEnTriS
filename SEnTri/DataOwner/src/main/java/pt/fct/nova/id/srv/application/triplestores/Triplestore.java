package pt.fct.nova.id.srv.application.triplestores;

import org.apache.jena.graph.Triple;
import pt.fct.nova.id.srv.application.InvalidNodeException;
import pt.fct.nova.id.srv.application.UnknownProtocolException;

import java.util.Iterator;
import java.util.Map;

public interface Triplestore {

    void createDataset(String storeID, Iterator<Triple> triples, Map<String, String> namespaces) throws InvalidNodeException, UnknownProtocolException;

    void uploadData(String storeID, Iterator<Triple> triples, Map<String, String> namespaces) throws UnknownProtocolException;
}
