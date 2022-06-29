package pt.fct.nova.id.srv.application.storage;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import pt.fct.nova.id.srv.application.query.jobs.VariablesPattern;
import pt.fct.nova.id.srv.application.storage.dao.TypedNode;

import java.util.List;
import java.util.Map;

public interface StorageEngine {

    boolean setupStore(String storeID, Map<String, String> namespaces);

    boolean deleteStore(String storeID);

    boolean saveTriple(String storeID, Triple triple);

    List<Triple> getTriples(String storeID);

    Map<String, String> getNamespaces(String storeID);

    List<Node> findSubjects(String storeID, Node predicate, Node object);

    List<Node> findPredicates(String storeID, Node subject, Node object);

    List<Node> findObjects(String storeID, Node subject, Node predicate);

    List<TypedNode> findSP(String storeID, Node object);

    List<TypedNode> findSO(String storeID, Node predicate);

    List<TypedNode> findPO(String storeID, Node subject);
}
