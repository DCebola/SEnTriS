package pt.fct.nova.id.srv.application.storage;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import pt.fct.nova.id.srv.application.query.jobs.VariablesPattern;
import pt.fct.nova.id.srv.application.storage.dao.TypedNode;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface StorageEngine {

    boolean setupStore(String storeID, Map<String, String> namespaces);

    boolean deleteStore(String storeID);

    boolean saveTriple(String storeID, Triple triple);

    List<Triple> getTriples(String storeID);

    Map<String, String> getNamespaces(String storeID);

    Set<String> findSubjects(String storeID, Node predicate, Node object);

    Set<String> findPredicates(String storeID, Node subject, Node object);

    Set<String> findObjects(String storeID, Node subject, Node predicate);

    Map<Var, Set<String>> findSP(String storeID, Node object, Var var1, Var var2);

    Map<Var, Set<String>> findSO(String storeID, Node predicate, Var var1, Var var2);

    Map<Var, Set<String>> findPO(String storeID, Node subject, Var var1, Var var2);

    Map<Var, Set<String>> findAll(String storeID, Var var1, Var var2, Var var3);

    List<Binding> getNodesAsBindings(String storeID, Map<Var, Set<String>> varIdxs);
}
