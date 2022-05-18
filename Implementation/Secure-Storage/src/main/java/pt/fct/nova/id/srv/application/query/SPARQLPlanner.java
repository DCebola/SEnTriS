package pt.fct.nova.id.srv.application.query;

import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpVisitorByTypeBase;
import org.apache.jena.sparql.algebra.OpWalker;
import org.apache.jena.sparql.algebra.op.*;

public class SPARQLPlanner extends OpVisitorByTypeBase {

    void opVisitorWalker(Op op) {
        OpWalker.walk(op, this);
    }

    @Override
    public void visit0(Op0 op0) {
        if (op0 instanceof OpBGP) {
            System.out.println("OP0: " + op0);
        } else if (op0 instanceof OpDatasetNames) {
            System.out.println("OP0: " + op0);
        } else if (op0 instanceof OpNull) {
            System.out.println("OP0: " + op0);
        } else if (op0 instanceof OpPath) {
            System.out.println("OP0: " + op0);
        } else if (op0 instanceof OpQuad) {
            System.out.println("OP0: " + op0);
        } else if (op0 instanceof OpQuadBlock) {
            System.out.println("OP0: " + op0);
        } else if (op0 instanceof OpQuadPattern) {
            System.out.println("OP0: " + op0);
        } else if (op0 instanceof OpTable) {
            System.out.println("OP0: " + op0);
        } else if (op0 instanceof OpTriple) {
            System.out.println("OP0: " + op0);
        }

    }


    @Override
    public void visit1(Op1 op1) {
        if (op1 instanceof OpExtendAssign) {
            System.out.println("OP1: " + op1);
        } else if (op1 instanceof OpFilter) {
            System.out.println("OP1: " + op1);
        } else if (op1 instanceof OpGraph) {
            System.out.println("OP1: " + op1);
        } else if (op1 instanceof OpGroup) {
            System.out.println("OP1: " + op1);
        } else if (op1 instanceof OpLabel) {
            System.out.println("OP1: " + op1);
        } else if (op1 instanceof OpModifier) {
            System.out.println("OP1: " + op1);
        } else if (op1 instanceof OpProcedure) {
            System.out.println("OP1: " + op1);
        } else if (op1 instanceof OpPropFunc) {
            System.out.println("OP1: " + op1);
        } else if (op1 instanceof OpService) {
            System.out.println("OP1: " + op1);
        }

    }

    @Override
    public void visit2(Op2 op2) {
        if (op2 instanceof OpConditional) {
            System.out.println("OP2: " + op2);
        } else if (op2 instanceof OpDiff) {
            System.out.println("OP2: " + op2);
        } else if (op2 instanceof OpJoin) {
            System.out.println("OP2: " + op2);
        } else if (op2 instanceof OpLeftJoin) {
            System.out.println("OP2: " + op2);
        } else if (op2 instanceof OpMinus) {
            System.out.println("OP2: " + op2);
        } else if (op2 instanceof OpUnion) {
            System.out.println("OP2: " + op2);
        }
    }

    @Override
    public void visitN(OpN opN) {
        if (opN instanceof OpDisjunction) {
            System.out.println("OPn: " + opN);
        } else if (opN instanceof OpSequence) {
            System.out.println("OPn: " + opN);
        }
    }

}
