package pt.fct.nova.id.srv.application.query;

import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpVisitorByTypeBase;
import org.apache.jena.sparql.algebra.OpWalker;
import org.apache.jena.sparql.algebra.op.*;
import pt.fct.nova.id.srv.application.query.jobs.GetJob;
import pt.fct.nova.id.srv.application.query.jobs.Job;
import pt.fct.nova.id.srv.application.query.jobs.VariablesPattern;

import java.util.*;

public class SPARQLPlanner extends OpVisitorByTypeBase {

    private final Map<Op, Set<Job>> jobs = new HashMap<>();
    private final Set<String> bindings = new HashSet<>();

    void opVisitorWalker(Op op) {
        OpWalker.walk(op, this);
    }

    @Override
    public void visit0(Op0 op) throws NotImplemented {
        System.out.println(op);
        List<Triple> triples;
        if (op instanceof OpBGP) {
            triples = ((OpBGP) op).getPattern().getList();
        } else if (op instanceof OpTriple) {
            triples = ((OpTriple) op).asBGP().getPattern().getList();
        } else
            throw new NotImplemented();
        triples.forEach(
                t -> {
                    Node s = t.getSubject();
                    Node p = t.getPredicate();
                    Node o = t.getObject();
                    GetJob j = new GetJob(extractVariablesPattern(s, p, o), s, p, o);
                    System.out.println(j.getVariablesPattern().toString());
                    Set<Job> op_jobs = jobs.get(op);
                    if (op_jobs == null)
                        op_jobs = new HashSet<>();
                    op_jobs.add(j);
                }
        );
    }

    private VariablesPattern extractVariablesPattern(Node subject, Node predicate, Node object) {
        if (!subject.isConcrete() && predicate.isConcrete() && object.isConcrete())
            return VariablesPattern.S;
        else if (subject.isConcrete() && !predicate.isConcrete() && object.isConcrete())
            return VariablesPattern.P;
        else if (subject.isConcrete() && predicate.isConcrete() && !object.isConcrete())
            return VariablesPattern.O;
        else if (!subject.isConcrete() && !predicate.isConcrete() && object.isConcrete())
            return VariablesPattern.SP;
        else if (!subject.isConcrete() && predicate.isConcrete() && !object.isConcrete())
            return VariablesPattern.SO;
        else if (subject.isConcrete() && !predicate.isConcrete() && !object.isConcrete())
            return VariablesPattern.PO;
        else
            return VariablesPattern.SPO;
    }


    @Override
    public void visit1(Op1 op) {

        if (op instanceof OpExtendAssign) {
            System.out.println("OP1 OpExtendAssign: " + op);
        } else if (op instanceof OpFilter) {
            System.out.println("OP1 OpFilter: " + op);
        } else if (op instanceof OpGraph) {
            System.out.println("OP1 OpGraph: " + op);
        } else if (op instanceof OpGroup) {
            System.out.println("OP1 OpGroup: " + op);
        } else if (op instanceof OpLabel) {
            System.out.println("OP1 OpLabel: " + op);
        } else if (op instanceof OpModifier) {
            System.out.println("OP1 OpModifier: " + op);
        } else if (op instanceof OpProcedure) {
            System.out.println("OP1 OpProcedure: " + op);
        } else if (op instanceof OpPropFunc) {
            System.out.println("OP1 OpPropFunc: " + op);
        } else if (op instanceof OpService) {
            System.out.println("OP1 OpService: " + op);
        }

    }

    @Override
    public void visit2(Op2 op) {

        if (op instanceof OpConditional) {
            System.out.println("OP2: " + op);
        } else if (op instanceof OpDiff) {
            System.out.println("OP2: " + op);
        } else if (op instanceof OpJoin) {
            System.out.println("OP2: " + op);
        } else if (op instanceof OpLeftJoin) {
            System.out.println("OP2: " + op);
        } else if (op instanceof OpMinus) {
            System.out.println("OP2: " + op);
        } else if (op instanceof OpUnion) {
            System.out.println("OP2: " + op);
        }
    }

    @Override
    public void visitN(OpN op) {
        if (op instanceof OpDisjunction) {
            System.out.println("OPn: " + op);
        } else if (op instanceof OpSequence) {
            System.out.println("OPn: " + op);
        }
    }

}
