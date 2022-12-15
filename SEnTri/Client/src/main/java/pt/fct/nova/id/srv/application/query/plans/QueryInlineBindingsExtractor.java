package pt.fct.nova.id.srv.application.query.plans;

import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpVisitorByType;
import org.apache.jena.sparql.algebra.OpWalker;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import pt.fct.nova.id.srv.application.query.Utils;
import pt.fct.nova.id.srv.application.query.jobs.VariablesPattern;

import java.util.*;

public class QueryInlineBindingsExtractor extends OpVisitorByType {

    private int totalBindings;

    public QueryInlineBindingsExtractor() {
        this.totalBindings = 0;
    }

    public int parse(Op op) {
        OpWalker.walk(op, this);
        return totalBindings;
    }


    @Override
    public void visit0(Op0 op) {
        if (op instanceof OpBGP opBGP) {
            generateGetJobs(opBGP);
        } else if (op instanceof OpTriple opTriple) {
            generateGetJobs(opTriple.asBGP());
        }
    }

    private void generateGetJobs(OpBGP op) {
        List<Triple> patterns = op.getPattern().getList();
        patterns.forEach(
                t -> {
                    Node s = t.getSubject();
                    Node p = t.getPredicate();
                    Node o = t.getObject();
                    incrementTotalBindings(Utils.extractVariablesPattern(s, p, o));
                }
        );
    }


    private void incrementTotalBindings(VariablesPattern pattern){
        switch (pattern) {
            case S, P, O -> totalBindings += 1;
            case SP, SO, PO -> totalBindings += 2;
            case SPO -> {
            }
        }
    }

    @Override
    public void visit1(Op1 op) {
    }

    @Override
    public void visit2(Op2 op) {
    }

    @Override
    protected void visitFilter(OpFilter opFilter) {
    }

    @Override
    protected void visitLeftJoin(OpLeftJoin opLeftJoin) {
    }

    @Override
    public void visitN(OpN op) {
    }
}
