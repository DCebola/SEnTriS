package pt.fct.nova.id.srv.application.storage;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;

import java.util.List;
import java.util.Map;

public interface StorageEngine {

    boolean setupStore(String storeID, Map<String, String> namespaces);

    boolean deleteStore(String storeID);

    boolean saveTriple(String storeID, Triple triple);

    List<Triple> getTriples(String storeID);

    Map<String, String> getNamespaces(String storeID);

    List<String> findSubjects(String storeID, Node predicate, Node object);

    List<String> findPredicates(String storeID, Node subject, Node object);

    List<String> findObjects(String storeID, Node subject, Node predicate);

    Map<Var, List<String>> findSP(String storeID, Node object, Var var1, Var var2);

    Map<Var, List<String>> findSO(String storeID, Node predicate, Var var1, Var var2);

    Map<Var, List<String>> findPO(String storeID, Node subject, Var var1, Var var2);

    Map<Var, List<String>> findAll(String storeID, Var var1, Var var2, Var var3);

    List<Binding> getNodesAsBindings(String storeID, Map<Var, List<String>> varIdxs);
}
