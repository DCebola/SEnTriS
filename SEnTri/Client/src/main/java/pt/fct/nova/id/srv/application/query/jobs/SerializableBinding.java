package pt.fct.nova.id.srv.application.query.jobs;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;

import java.io.Serial;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;

public class SerializableBinding implements Serializable {
    @Serial
    private static final long serialVersionUID = 1345655033362467694L;
    private final Map<Var, Node> values;


    public SerializableBinding(Map<Var, Node> values) {
        this.values = values;
    }

    public Iterator<Var> vars() {
        return values.keySet().iterator();
    }

    public Node get(Var var) {
        return values.get(var);
    }
}
