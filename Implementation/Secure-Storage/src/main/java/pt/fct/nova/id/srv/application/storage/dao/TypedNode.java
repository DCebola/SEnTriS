package pt.fct.nova.id.srv.application.storage.dao;

import org.apache.jena.graph.Node;
import pt.fct.nova.id.srv.application.query.jobs.VariablesPattern;

public class TypedNode {

    private final VariablesPattern type;
    private final Node node;

    public TypedNode(VariablesPattern type, Node node) {
        this.type = type;
        this.node = node;
    }

    public VariablesPattern getType() {
        return type;
    }

    public Node getNode() {
        return node;
    }
}
