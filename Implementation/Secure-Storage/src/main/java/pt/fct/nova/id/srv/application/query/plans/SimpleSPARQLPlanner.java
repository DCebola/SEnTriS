package pt.fct.nova.id.srv.application.query.plans;

import org.apache.jena.graph.Node;
import org.apache.jena.query.QueryBuildException;
import org.apache.jena.query.SortCondition;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpVisitorByTypeBase;
import org.apache.jena.sparql.algebra.OpWalker;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import pt.fct.nova.id.srv.application.query.jobs.*;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.*;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.*;

import java.util.*;

import static pt.fct.nova.id.srv.application.Utils.*;

public class SimpleSPARQLPlanner extends OpVisitorByTypeBase implements SPARQLPlanner {

    private final Map<Op, String> parsed_op;

    private final SimpleExecutionPlan plan;

    public SimpleSPARQLPlanner() {
        this.parsed_op = new HashMap<>();
        this.plan = new SimpleExecutionPlan();
    }

    public ExecutionPlan generatePlan(Op op, List<String> resultVarNames) {
        OpWalker.walk(op, this);
        plan.setVars(generateVars(resultVarNames));
        return plan;
    }

    private List<Var> generateVars(List<String> resultVarNames) {
        List<Var> vars = new LinkedList<>();
        resultVarNames.forEach(v -> vars.add(Var.alloc(v)));
        return vars;
    }

    @Override
    public void visit0(Op0 op) {
        System.out.println("OP0: " + op);
        if (op instanceof OpBGP) {
            generateGetJobs((OpBGP) op);
        } else if (op instanceof OpTriple) {
            generateGetJobs(((OpTriple) op).asBGP());
        } else if (op instanceof OpTable) {
            generateValuesJob((OpTable) op);
        } /* else if (op instanceof OpDatasetNames) {
        } else if (op instanceof OpPath) {
        } else if (op instanceof OpNull) {
        } else if (op instanceof OpQuad) {
        } else if (op instanceof OpQuadBlock) {
        } else if (op instanceof OpQuadPattern) {
        } else
            throw new NotImplemented();
        */
    }

    private void generateGetJobs(OpBGP op) {
        op.getPattern().getList().forEach(
                t -> {
                    Node s = t.getSubject();
                    Node p = t.getPredicate();
                    Node o = t.getObject();
                    String jobID = generateID();
                    parsed_op.put(op, jobID);
                    plan.pushJob(new GetJob(jobID, extractVariablesPattern(s, p, o), s, p, o));
                }
        );
    }

    private void generateValuesJob(OpTable op) {
        List<Binding> values = new LinkedList<>();
        op.getTable().rows().forEachRemaining(values::add);
        String jobID = generateID();
        parsed_op.put(op, jobID);
        plan.pushJob(new ValuesJob(jobID, values));
    }

    @Override
    public void visit1(Op1 op) {
        System.out.println("OP1: " + op);
        if (op instanceof OpExtendAssign) {
            generateBindJob((OpExtendAssign) op);
        } else if (op instanceof OpFilter) {
            generateFilterJob((OpFilter) op);
        } else if (op instanceof OpGroup) {
            generateGroupJob((OpGroup) op);
        } else if (op instanceof OpModifier) {
            visitOpModifier((OpModifier) op);
        }/* else if (op instanceof OpGraph) {
        } else if (op instanceof OpLabel) {
        } else if (op instanceof OpProcedure) {
        } else if (op instanceof OpPropFunc) {
        } else if (op instanceof OpService) {
        } else
            throw new NotImplemented();
        */
    }


    private void generateBindJob(OpExtendAssign op) {
        String prevJobID = getPrevJobID(op.getSubOp());
        String jobID = generateID();
        op.getVarExprList().getExprs().forEach(
                (var, expr) -> plan.pushJob(new BindJob(jobID, prevJobID, var, expr))
        );
        parsed_op.put(op, jobID);

    }

    private String getPrevJobID(Op subOp) {
        String prevJobID = parsed_op.get(subOp);
        if (prevJobID == null)
            throw new QueryBuildException();
        return prevJobID;
    }

    private void generateFilterJob(OpFilter op) {
        String jobID = generateID();
        plan.pushJob(new FilterJob(jobID, getPrevJobID(op.getSubOp()), op.getExprs().getList()));
        parsed_op.put(op, jobID);
    }


    private void generateGroupJob(OpGroup op) {
        String jobID = generateID();
        plan.pushJob(new GroupJob(jobID, getPrevJobID(op.getSubOp()), op.getAggregators()));
        parsed_op.put(op, jobID);
    }


    private void visitOpModifier(OpModifier op) {
        if (op instanceof OpDistinctReduced) {
            generateDistinctJob((OpDistinctReduced) op);
        } else if (op instanceof OpOrder) {
            generateOrderByJob((OpOrder) op);
        } else if (op instanceof OpProject) {
            generateProjectJob((OpProject) op);
        } else if (op instanceof OpSlice) {
            generateSliceJob((OpSlice) op);
        }/* else if (op instanceof OpTopN) {
        }  else if (op instanceof OpList) {
        }  else
             throw new NotImplemented();
        */
    }

    private void generateProjectJob(OpProject op) {
        String jobID = generateID();
        plan.pushJob(new ProjectJob(jobID, getPrevJobID(op.getSubOp()), op.getVars()));
        parsed_op.put(op, jobID);
    }

    private void generateDistinctJob(OpDistinctReduced op) {
        String jobID = generateID();
        plan.pushJob(new DistinctJob(jobID, getPrevJobID(op.getSubOp())));
        parsed_op.put(op, jobID);
    }

    private void generateOrderByJob(OpOrder op) {
        for (SortCondition condition : op.getConditions()) {
            OrderByDirection dir = extractOrderDirection(condition.getDirection());
            if (dir == null)
                throw new QueryBuildException();
        }
        String jobID = generateID();
        plan.pushJob(new OrderByJob(jobID, getPrevJobID(op.getSubOp()), op.getConditions()));
        parsed_op.put(op, jobID);
    }

    private void generateSliceJob(OpSlice op) {
        String jobID = generateID();
        plan.pushJob(new SliceJob(jobID, op.getStart(), op.getLength()));
        parsed_op.put(op, jobID);
    }


    @Override
    public void visit2(Op2 op) {
        System.out.println("OP2: " + op);
        if (op instanceof OpJoin) {
            generateJoinJob((OpJoin) op);
        } else if (op instanceof OpLeftJoin) {
            generateOptionalJob((OpLeftJoin) op);
        } else if (op instanceof OpMinus) {
            generateMinusJob((OpMinus) op);
        } else if (op instanceof OpUnion) {
            generateUnionJob((OpUnion) op);
        }/* else if (op instanceof OpConditional) {
        } else if (op instanceof OpDiff) {
        } else
            throw new NotImplemented();
          */
    }

    private void generateJoinJob(OpJoin op) {
        String jobID = generateID();
        plan.pushJob(new JoinJob(jobID, getPrevJobID(op.getLeft()), getPrevJobID(op.getRight())));
        parsed_op.put(op, jobID);
    }

    private void generateOptionalJob(OpLeftJoin op) {
        String jobID = generateID();
        plan.pushJob(new OptionalJob(jobID, getPrevJobID(op.getLeft()), getPrevJobID(op.getRight())));
        parsed_op.put(op, jobID);
    }

    private void generateMinusJob(OpMinus op) {
        String jobID = generateID();
        plan.pushJob(new MinusJob(jobID, getPrevJobID(op.getLeft()), getPrevJobID(op.getRight())));
        parsed_op.put(op, jobID);
    }

    private void generateUnionJob(OpUnion op) {
        String jobID = generateID();
        plan.pushJob(new UnionJob(jobID, getPrevJobID(op.getLeft()), getPrevJobID(op.getRight())));
        parsed_op.put(op, jobID);
    }

    @Override
    public void visitN(OpN op) {
        /*
        if (op instanceof OpDisjunction) {
        } else if (op instanceof OpSequence) {
        } else
           throw new NotImplemented();
         */
    }

}
