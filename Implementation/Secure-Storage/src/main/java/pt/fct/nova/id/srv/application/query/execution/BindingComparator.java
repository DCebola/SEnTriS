package pt.fct.nova.id.srv.application.query.execution;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.resultset.ResultSetCompare;
import org.apache.jena.sparql.util.NodeUtils;


import java.util.Comparator;

public class BindingComparator implements Comparator<Binding> {

    private final Var var;

    public BindingComparator(Var var) {
        this.var = var;
    }

    @Override
    public int compare(Binding b1, Binding b2) {
        Node n1 = b1.get(var);
        Node n2 = b2.get(var);

        if (n1 == null && n2 == null)
            return 0;
        else if (n1 != null && n2 == null)
            return 1;
        else if (n1 == null && n2 != null)
            return -1;
        else{
            ResultSetCompare.equal(b1, b2, NodeUtils.sameValue);
        }

        return 0;
    }

}
