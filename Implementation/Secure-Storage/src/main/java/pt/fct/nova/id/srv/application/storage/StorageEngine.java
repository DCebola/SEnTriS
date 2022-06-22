package pt.fct.nova.id.srv.application.storage;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import pt.fct.nova.id.srv.application.query.jobs.VariablesPattern;
import pt.fct.nova.id.srv.application.storage.dao.TypedNode;

import java.util.Map;

public interface StorageEngine {

    boolean setupStore(String storeID, Map<String, String> namespaces);

    boolean deleteStore(String storeID);

    boolean saveTriple(String storeID, Triple triple);

    Iterable<Triple> getTriples(String storeID);

    Map<String, String> getNamespaces(String storeID);

    Iterable<Node> findSubjects(Node predicate, Node object);

    Iterable<Node> findPredicates(Node subject, Node object);

    Iterable<Node> findObjects(Node subject, Node predicate);

    Iterable<TypedNode> findSP(Node object);

    Iterable<TypedNode> findSO(Node predicate);

    Iterable<TypedNode> findPO(Node subject);
}
