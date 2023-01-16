package pt.fct.nova.id.srv.application.storage;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import pt.fct.nova.id.srv.application.storage.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.storage.iri_tables.IRITable;

import java.util.Set;

public interface StorageEngine {

    void delete(String triplestoreID);

    void delete(String triplestoreID, Set<String[]> triples);

    void save(String triplestoreID, Set<String[]> triples);

    IRITable findS(String triplestoreID, Node predicate, Node object, Var subject) throws InvalidNodeException;

    IRITable findP(String triplestoreID, Node subject, Node object, Var predicate) throws InvalidNodeException;

    IRITable findO(String triplestoreID, Node subject, Node predicate, Var object) throws InvalidNodeException;

    IRITable findSP(String triplestoreID, Node object, Var subject, Var predicate) throws InvalidNodeException;

    IRITable findSO(String triplestoreID, Node predicate, Var subject, Var object) throws InvalidNodeException;

    IRITable findPO(String triplestoreID, Node subject, Var predicate, Var object) throws InvalidNodeException;

    Node generateNode(String iri);

    String parseNodeIRI(Node node) throws InvalidNodeException;


}
