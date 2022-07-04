package pt.fct.nova.id.srv.application.storage;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import pt.fct.nova.id.srv.application.storage.idx_tables.IdxTable;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface StorageEngine {

    boolean setupStore(String storeID, Map<String, String> namespaces);

    boolean deleteStore(String storeID);

    boolean saveTriple(String storeID, Triple triple);

    List<Triple> getTriples(String storeID);

    Map<String, String> getNamespaces(String storeID);

    IdxTable findSubjects(String storeID, Node predicate, Node object, Var var);

    IdxTable findPredicates(String storeID, Node subject, Node object, Var var);

    IdxTable findObjects(String storeID, Node subject, Node predicate, Var var);

    IdxTable2 findSP(String storeID, Node object, Var var1, Var var2);

    IdxTable2 findSO(String storeID, Node predicate, Var var1, Var var2);

    IdxTable2 findPO(String storeID, Node subject, Var var1, Var var2);

    IdxTable3 findAll(String storeID, Var var1, Var var2, Var var3);

    List<Binding> getNodesAsBindings(String storeID, Map<Var, Set<String>> varIdxs);
}
