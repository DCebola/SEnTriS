package pt.fct.nova.id.srv.application.triplestores;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import pt.fct.nova.id.srv.application.InvalidNodeException;
import pt.fct.nova.id.srv.application.UnknownProtocolException;

import java.util.Iterator;
import java.util.Map;

public class EncryptedTriplestore implements Triplestore {

    private static final String IRI_SEPARATOR = System.getenv("IRI_SEPARATOR");
    private final static String BLANK_IRI = "B".concat(IRI_SEPARATOR).concat("%s");
    private static final String SIMPLE_IRI = "S".concat(IRI_SEPARATOR).concat("%s");
    private static final String LITERAL_IRI = "L".concat(IRI_SEPARATOR).concat("%s").concat(IRI_SEPARATOR).concat("%s");

    private static final String PROTOCOL = System.getenv("PROTOCOL");


    private String parseNodeIRI(Node node) throws InvalidNodeException {
        if (!node.isConcrete())
            throw new InvalidNodeException();
        if (node.isURI())
            return String.format(SIMPLE_IRI, node.getURI());
        else if (node.isLiteral())
            return String.format(LITERAL_IRI, node.getLiteralLexicalForm(), node.getLiteralDatatypeURI());
        else
            return String.format(BLANK_IRI, node.getBlankNodeId());
    }

    @Override
    public void createDataset(String storeID, Iterator<Triple> triples, Map<String, String> namespaces) throws UnknownProtocolException {
        if (PROTOCOL.equals("PROTOCOL_1"))
            createDatasetUsingOnions(storeID, triples);
        else if (PROTOCOL.equals("PROTOCOL_2")) {
            createDatasetUsingDGK(storeID, triples);
        } else throw new UnknownProtocolException();
    }


    @Override
    public void uploadData(String storeID, Iterator<Triple> triples, Map<String, String> namespaces) throws UnknownProtocolException {
        if (PROTOCOL.equals("PROTOCOL_1"))
            uploadDataUsingOnions(storeID, triples);
        else if (PROTOCOL.equals("PROTOCOL_2")) {
            uploadDataUsingDGK(storeID, triples);
        } else throw new UnknownProtocolException();
    }

}
