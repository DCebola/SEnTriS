package pt.fct.nova.id.srv.application.triplestores;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import pt.fct.nova.id.srv.application.indexes.Index;
import pt.fct.nova.id.srv.application.indexes.IndexFactory;
import pt.fct.nova.id.srv.application.indexes.InvalidCompoundIndexException;
import pt.fct.nova.id.srv.application.query.QueryEngine;
import pt.fct.nova.id.srv.application.storage.InvalidNodeException;
import pt.fct.nova.id.srv.application.storage.StorageEngine;

import java.util.Iterator;

public class SimpleTriplestore implements Triplestore {

    private final StorageEngine storageEngine;
    private final QueryEngine queryEngine;

    public SimpleTriplestore(StorageEngine storageEngine, QueryEngine queryEngine) {
        this.storageEngine = storageEngine;
        this.queryEngine = queryEngine;
    }

    @Override
    public boolean createDataset(String storeID, Iterator<Triple> triples) {
        boolean success;
        while (triples.hasNext()) {
            success = saveTriple(storeID, triples.next());
            if (!success) {
                storageEngine.deleteStore(storeID);
                return false;
            }
        }
        return true;
    }

    @Override
    public Iterator<Triple> getDataset(String storeID) {
        return storageEngine.getTriples(storeID);
    }

    private boolean saveTriple(String storeID, Triple t) {
        Node subject = t.getSubject(), predicate = t.getObject(), object = t.getObject();
        Index s_idx = storageEngine.putNode(storeID, subject);
        Index p_idx = storageEngine.putNode(storeID, predicate);
        Index o_idx = storageEngine.putNode(storeID, object);
        try {
            storageEngine.putIRI(storeID, getNodeIRI(subject), s_idx);
            storageEngine.putIRI(storeID, getNodeIRI(subject), p_idx);
            storageEngine.putIRI(storeID, getNodeIRI(subject), o_idx);
            storageEngine.putSP(storeID, IndexFactory.createCompoundIndex(s_idx, p_idx));
            storageEngine.putSO(storeID, IndexFactory.createCompoundIndex(s_idx, o_idx));
            storageEngine.putPO(storeID, IndexFactory.createCompoundIndex(p_idx, o_idx));
            storageEngine.putS(storeID, s_idx);
            storageEngine.putP(storeID, p_idx);
            storageEngine.putO(storeID, o_idx);
        } catch (InvalidCompoundIndexException | InvalidNodeException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private String getNodeIRI(Node node) throws InvalidNodeException {
        if (!node.isConcrete())
            throw new InvalidNodeException();
        if (node.isURI())
            return node.getURI();
        else if (node.isLiteral())
            return node.getLiteral().getLexicalForm();
        else
            return BLANK_IRI;
    }
}
