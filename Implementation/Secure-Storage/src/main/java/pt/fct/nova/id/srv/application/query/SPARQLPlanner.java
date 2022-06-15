package pt.fct.nova.id.srv.application.query;

import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpVisitorByTypeBase;
import org.apache.jena.sparql.algebra.OpWalker;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.engine.binding.Binding;
import pt.fct.nova.id.srv.application.query.jobs.BindJob;
import pt.fct.nova.id.srv.application.query.jobs.GetJob;
import pt.fct.nova.id.srv.application.query.jobs.Job;

import java.util.*;

import static pt.fct.nova.id.srv.application.Utils.extractVariablesPattern;
import static pt.fct.nova.id.srv.application.Utils.generateID;

public class SPARQLPlanner extends OpVisitorByTypeBase {

    private final Deque<Job> plan = new LinkedList<>();

    void opVisitorWalker(Op op) {
        OpWalker.walk(op, this);
    }

    public Queue<Job> getPlan() {
        return plan;
    }

    @Override
    public void visit0(Op0 op) throws NotImplemented {
        System.out.println("OP0: " + op);
        List<Triple> triples;
        if (op instanceof OpBGP) {
            triples = ((OpBGP) op).getPattern().getList();
            generateGetJobs(triples);
        } else if (op instanceof OpTriple) {
            triples = ((OpTriple) op).asBGP().getPattern().getList();
            generateGetJobs(triples);
        } else if (op instanceof OpTable) {
            generateValuesJob(((OpTable) op).getTable().rows());
        } /* else
            throw new NotImplemented();
          */
    }

    private void generateValuesJob(Iterator<Binding> bindings) {

    }

    private void generateGetJobs(List<Triple> triples) {
        triples.forEach(
                t -> {
                    Node s = t.getSubject();
                    Node p = t.getPredicate();
                    Node o = t.getObject();
                    plan.addFirst(new GetJob(generateID(), extractVariablesPattern(s, p, o), s, p, o));
                }
        );
    }

    @Override
    public void visit1(Op1 op) {
        if (op instanceof OpExtendAssign) {
            System.out.println("OP1 OpExtendAssign: " + op);
            System.out.println(((OpExtendAssign) op).getVarExprList().getExprs());
        } else if (op instanceof OpModifier) {
            visitOpModifier((OpModifier) op);
            System.out.println("OP1 Modifier: " + op);
        } else if (op instanceof OpFilter) {
            System.out.println(((OpFilter) op).getExprs());
            System.out.println("OP1 OpFilter: " + op);
        } else if (op instanceof OpGroup) {
            System.out.println("OP1 OpGroup: " + op);
            System.out.println(((OpGroup) op).getGroupVars().getExprs());
            System.out.println(((OpGroup) op).getAggregators());
        }/* else if (op instanceof OpGraph) {
            System.out.println("OP1 OpGraph: " + op);
        } else if (op instanceof OpLabel) {
            System.out.println("OP1 Label: " + op);
        } else if (op instanceof OpProcedure) {
            System.out.println("OP1 OpProcedure: " + op);
        } else if (op instanceof OpPropFunc) {
            System.out.println("OP1 OpPropFunc: " + op);
        } else if (op instanceof OpService) {
            System.out.println("OP1 OpService: " + op);
        } else
            throw new NotImplemented();
        */
    }

    private void visitOpModifier(OpModifier op) {
        if (op instanceof OpDistinct) {

        } else if (op instanceof OpReduced) {

        } else if (op instanceof OpOrder) {

        } else if (op instanceof OpProject) {

        } else if (op instanceof OpSlice) {

        } /* else if (op instanceof OpTopN) {

        }  else if (op instanceof OpList) {

        } */
    }

    private void generateBindJob(VarExprList exprList) {
        exprList.getExprs().forEach(
                (var, expr) -> plan.addFirst(new BindJob(generateID(), var, expr))
        );
    }

    @Override
    public void visit2(Op2 op) {
        if (op instanceof OpJoin) {
            System.out.println("OP2: " + op);
        } else if (op instanceof OpLeftJoin) {
            System.out.println("OP2: " + op);
        } else if (op instanceof OpMinus) {
            System.out.println("OP2: " + op);
        } else if (op instanceof OpUnion) {
            System.out.println("OP2: " + op);
        } /* else if (op instanceof OpConditional) {
            System.out.println("OP2: " + op);
        } else if (op instanceof OpDiff) {
            System.out.println("OP2: " + op);
        } else
            throw new NotImplemented();
          */
    }

    @Override
    public void visitN(OpN op) {
        /*
        if (op instanceof OpDisjunction) {
            System.out.println("OPN OpDisjunction: " + op);
        } else if (op instanceof OpSequence) {
            System.out.println("OPN OpSequence: " + op);
        } else
           throw new NotImplemented();
         */
    }

}
