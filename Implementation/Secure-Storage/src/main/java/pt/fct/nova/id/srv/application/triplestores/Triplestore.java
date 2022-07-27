package pt.fct.nova.id.srv.application.triplestores;

import org.apache.jena.graph.Triple;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import pt.fct.nova.id.srv.application.query.execution.exceptions.SPARQLExecutionException;
import pt.fct.nova.id.srv.application.storage.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.storage.exceptions.StoreAlreadyExistsException;
import pt.fct.nova.id.srv.application.storage.exceptions.StoreNotFoundException;

import java.util.Iterator;
import java.util.Map;

public interface Triplestore {

    void createDataset(String storeID, Iterator<Triple> triples, Map<String, String> namespaces) throws StoreAlreadyExistsException, InvalidNodeException;

    Model getDatasetModel(String storeID) throws StoreNotFoundException;

    ResultSet executeQuery(String storeID, String query) throws SPARQLExecutionException, StoreNotFoundException, InvalidNodeException;
}
