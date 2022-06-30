package pt.fct.nova.id.srv.application.storage;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Var;
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

    Map<Var, List<Node>> findSubjects(String storeID, Node predicate, Node object, Var var);

    Map<Var, List<Node>> findPredicates(String storeID, Node subject, Node object, Var var);

    Map<Var, List<Node>> findObjects(String storeID, Node subject, Node predicate, Var var);

    Map<Var, List<Node>> findSP(String storeID, Node object, Var var1, Var var2);

    Map<Var, List<Node>> findSO(String storeID, Node predicate, Var var1, Var var2);

    Map<Var, List<Node>> findPO(String storeID, Node subject, Var var1, Var var2);

    Map<Var, List<Node>> findAll(String storeID, Var var1, Var var2, Var var3);
}
