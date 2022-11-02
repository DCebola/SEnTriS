package pt.fct.nova.id.srv.application.storage;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Var;
import pt.fct.nova.id.srv.application.storage.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.storage.exceptions.StorageEngineException;
import pt.fct.nova.id.srv.application.storage.exceptions.StoreAlreadyExistsException;
import pt.fct.nova.id.srv.application.storage.exceptions.StoreNotFoundException;
import pt.fct.nova.id.srv.application.storage.iri_tables.IRITable;
import redis.clients.jedis.exceptions.JedisException;

import java.util.List;
import java.util.Map;

public interface StorageEngine {

    void setupStore(String storeID, Map<String, String> namespaces) throws StorageEngineException;

    void saveNamespaces(String storeID, Map<String, String> namespaces);

    void deleteStore(String storeID) throws StorageEngineException;

    void saveTriple(String storeID, Triple triple) throws InvalidNodeException, StorageEngineException;

    List<Triple> getTriples(String storeID);

    Map<String, String> getNamespaces(String storeID);

    IRITable findSubjects(String storeID, Node predicate, Node object, Var var);

    IRITable findPredicates(String storeID, Node subject, Node object, Var var);

    IRITable findObjects(String storeID, Node subject, Node predicate, Var var);

    IRITable findSP(String storeID, Node object, Var var1, Var var2);

    IRITable findSO(String storeID, Node predicate, Var var1, Var var2);

    IRITable findPO(String storeID, Node subject, Var var1, Var var2);

    IRITable findAll(String storeID, Var var1, Var var2, Var var3);

    String parseNodeIRI(Node node) throws InvalidNodeException;

    Node generateNode(String iri);

    void checkID(String storeID) throws StoreAlreadyExistsException, StoreNotFoundException;

}
