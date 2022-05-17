package pt.fct.nova.id.srv.application.query;

import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.algebra.walker.OpVisitorByType;

public class SPARQLVisitor implements OpVisitorByType {


    @Override
    public void DUMMY() {
        //DUMMY
    }

    @Override
    public void visit0(Op0 op0) {
        switch (op0) {
            case OpBGP opBGP -> System.out.println(opBGP);
            case OpDatasetNames opDatasetNames -> System.out.println(opDatasetNames);
            case OpNull OpNull -> System.out.println(OpNull);
            case OpPath OpPath -> System.out.println(OpPath);
            case OpQuad OpQuad -> System.out.println(OpQuad);
            case OpQuadBlock OpQuadBlock -> System.out.println(OpQuadBlock);
            case OpQuadPattern OpQuadPattern -> System.out.println(OpQuadPattern);
            case OpTable OpTable -> System.out.println(OpTable);
            case OpTriple OpTriple -> System.out.println(OpTriple);
            case null, default -> {
            }
        }

    }


    @Override
    public void visit1(Op1 op1) {
        switch (op1) {
            case OpExtendAssign OpExtendAssign -> System.out.println(OpExtendAssign);
            case OpFilter OpFilter -> System.out.println(OpFilter);
            case OpGraph OpGraph -> System.out.println(OpGraph);
            case OpGroup OpGroup -> System.out.println(OpGroup);
            case OpLabel OpLabel -> System.out.println(OpLabel);
            case OpModifier OpModifier -> System.out.println(OpModifier);
            case OpProcedure OpProcedure -> System.out.println(OpProcedure);
            case OpPropFunc OpPropFunc -> System.out.println(OpPropFunc);
            case OpService OpService -> System.out.println(OpService);
            case null, default -> {
            }
        }

    }

    @Override
    public void visit2(Op2 op2) {
        switch (op2) {
            case OpConditional OpConditional -> System.out.println(OpConditional);
            case OpDiff OpDiff -> System.out.println(OpDiff);
            case OpJoin OpJoin -> System.out.println(OpJoin);
            case OpLeftJoin OpLeftJoin -> System.out.println(OpLeftJoin);
            case OpMinus OpMinus -> System.out.println(OpMinus);
            case OpUnion OpUnion -> System.out.println(OpUnion);
            case null, default -> {
            }
        }
    }

    @Override
    public void visitN(OpN opN) {
        switch (opN) {
            case OpDisjunction OpDisjunction -> System.out.println(OpDisjunction);
            case OpSequence OpSequence -> System.out.println(OpSequence);
            case null, default -> {
            }
        }
    }

}
