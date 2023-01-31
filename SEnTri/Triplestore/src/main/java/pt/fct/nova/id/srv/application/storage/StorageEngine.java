package pt.fct.nova.id.srv.application.storage;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.sparql.core.Var;
import pt.fct.nova.id.srv.application.storage.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.storage.tables.BindingsTable;

import java.util.Set;

public interface StorageEngine {

    void delete(String triplestoreID, boolean schema);

    void delete(String triplestoreID, Set<Triple> triples) throws InvalidNodeException;

    void save(String triplestoreID, Set<Triple> triples) throws InvalidNodeException;
    void saveSchema(String triplestoreID, Set<Triple> triples) throws InvalidNodeException;
    Set<Triple> findSchema(String triplestoreID);

    BindingsTable findS(String triplestoreID, Node predicate, Node object, Var subject) throws InvalidNodeException;

    BindingsTable findP(String triplestoreID, Node subject, Node object, Var predicate) throws InvalidNodeException;

    BindingsTable findO(String triplestoreID, Node subject, Node predicate, Var object) throws InvalidNodeException;

    BindingsTable findSP(String triplestoreID, Node object, Var subject, Var predicate) throws InvalidNodeException;

    BindingsTable findSO(String triplestoreID, Node predicate, Var subject, Var object) throws InvalidNodeException;

    BindingsTable findPO(String triplestoreID, Node subject, Var predicate, Var object) throws InvalidNodeException;

    Node generateNode(String iri);

    String parseNode(Node node) throws InvalidNodeException;


}
