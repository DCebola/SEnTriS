package pt.fct.nova.id.srv.application.storage;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Var;
import pt.fct.nova.id.srv.application.storage.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.storage.iri_tables.IRITable;

import java.util.List;

public interface StorageEngine {

    void deleteStore(String storeID);

    void saveTriples(String storeID, List<Triple> triples) throws InvalidNodeException;

    IRITable findSubjects(String storeID, Node predicate, Node object, Var var);

    IRITable findPredicates(String storeID, Node subject, Node object, Var var);

    IRITable findObjects(String storeID, Node subject, Node predicate, Var var);

    IRITable findSP(String storeID, Node object, Var var1, Var var2);

    IRITable findSO(String storeID, Node predicate, Var var1, Var var2);

    IRITable findPO(String storeID, Node subject, Var var1, Var var2);

    IRITable findAll(String storeID, Var var1, Var var2, Var var3);

    String parseNodeIRI(Node node) throws InvalidNodeException;

    Node generateNode(String iri);

}
