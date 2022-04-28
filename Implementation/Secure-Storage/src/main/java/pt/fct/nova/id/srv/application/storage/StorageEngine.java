package pt.fct.nova.id.srv.application.storage;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import pt.fct.nova.id.srv.application.indexes.Index;

import java.util.Iterator;
import java.util.Set;

public interface StorageEngine {


    Set<Node> getNodes(String storeID, Set<Index> idxs);

    Iterator<Triple> getTriples(String storeID);

    Node getNode(String storeID, Index idx);

    Set<Index> findByIRI(String storeID, String nodeIRI);

    Set<Index> findByP(String storeID, Index cpIdx);

    Set<Index> findByS(String storeID, Index cpIdx);

    Set<Index> findByO(String storeID, Index cpIdx);

    Set<Index> findBySP(String storeID, Index cpIdx);

    Set<Index> findBySO(String storeID, Index cpIdx);

    Set<Index> findByPO(String storeID, Index cpIdx);

    Set<Index> getS(String storeID);

    Set<Index> getP(String storeID);

    Set<Index> getO(String storeID);

    Set<Index> getSP(String storeID);

    Set<Index> getSO(String storeID);

    Set<Index> getPO(String storeID);

    Index putNode(String storeID, Node node);

    boolean putIRI(String storeID, String nodeIRI, Index idx);

    boolean putS(String storeID, Index idx);

    boolean putP(String storeID, Index idx);

    boolean putO(String storeID, Index idx);

    boolean putSP(String storeID, Index idx);

    boolean putSO(String storeID, Index idx);

    boolean putPO(String storeID, Index idx);

    void deleteStore(String storeID);
}
