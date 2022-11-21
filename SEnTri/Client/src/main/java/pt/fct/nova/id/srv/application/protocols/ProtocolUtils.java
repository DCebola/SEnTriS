package pt.fct.nova.id.srv.application.protocols;

import org.apache.jena.graph.Node;
import pt.fct.nova.id.srv.application.protocols.exceptions.InvalidNodeException;

public class ProtocolUtils {
    private static final String IRI_SEPARATOR = System.getenv("IRI_SEPARATOR");
    private static final String BLANK_IRI = "BLANK";
    private static final String SIMPLE_IRI = "S".concat(IRI_SEPARATOR).concat("%s");
    private static final String LITERAL_IRI = "L".concat(IRI_SEPARATOR).concat("%s").concat(IRI_SEPARATOR).concat("%s");

    public static String parseNodeIRI(Node node) throws InvalidNodeException {
        if (!node.isConcrete())
            throw new InvalidNodeException();
        if (node.isURI())
            return String.format(SIMPLE_IRI, node.getURI());
        else if (node.isLiteral())
            return String.format(LITERAL_IRI, node.getLiteralLexicalForm(), node.getLiteralDatatypeURI());
        else
            return BLANK_IRI;
    }
}
