package pt.fct.nova.id.srv.application.storage.clients;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import pt.fct.nova.id.srv.application.storage.indexes.Index;

import java.util.Iterator;
import java.util.Set;

public interface StorageClient {

    Set<Index> findNode(String storeID, String uri);

    Set<Node> getNodes(String storeID, Set<Index> idxs);
    Iterator<Triple> getTriples(String storeID);

    Node getNode(String storeID, Index idx);

    /* Find index from complementary index */

    Set<Index> findP(String storeID, Index cpIdx);

    Set<Index> findS(String storeID, Index cpIdx);

    Set<Index> findO(String storeID, Index cpIdx);

    Set<Index> findSP(String storeID, Index cpIdx);

    Set<Index> findSO(String storeID, Index cpIdx);

    Set<Index> findPO(String storeID, Index cpIdx);

    /* Get index table */

    Set<Index> getS(String storeID);

    Set<Index> getP(String storeID);

    Set<Index> getO(String storeID);

    Set<Index> getSP(String storeID);

    Set<Index> getSO(String storeID);

    Set<Index> getPO(String storeID);

    /* Insert new indexes */

    boolean putS(String storeID, Index idx);

    boolean putP(String storeID, Index idx);

    boolean putO(String storeID, Index idx);

    boolean putSP(String storeID, Index idx);

    boolean putSO(String storeID, Index idx);

    boolean putPO(String storeID, Index idx);

}
