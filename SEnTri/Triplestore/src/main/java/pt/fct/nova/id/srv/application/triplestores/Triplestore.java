package pt.fct.nova.id.srv.application.triplestores;

import org.apache.jena.graph.Triple;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import pt.fct.nova.id.srv.application.storage.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.storage.exceptions.StoreAlreadyExistsException;
import pt.fct.nova.id.srv.application.storage.exceptions.StoreNotFoundException;

import java.util.List;
import java.util.Map;
public interface Triplestore {

    void createDataset(String storeID, List<Triple> triples, Map<String, String> namespaces) throws StoreAlreadyExistsException, InvalidNodeException;
    void uploadData(String storeID, List<Triple> triples, Map<String, String> namespaces) throws StoreNotFoundException, InvalidNodeException;

    Model getDatasetModel(String storeID);

    ResultSet executeQuery(String storeID, String query);

    void delete(String storeID);
}
