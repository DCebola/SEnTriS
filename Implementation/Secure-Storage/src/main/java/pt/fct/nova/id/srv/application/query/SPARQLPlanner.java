package pt.fct.nova.id.srv.application.query;

import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.JoinType;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpVisitorByTypeBase;
import org.apache.jena.sparql.algebra.OpWalker;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.engine.binding.Binding;
import pt.fct.nova.id.srv.application.query.jobs.BindJob;
import pt.fct.nova.id.srv.application.query.jobs.GetJob;
import pt.fct.nova.id.srv.application.query.jobs.Job;
import pt.fct.nova.id.srv.application.query.jobs.ValuesJob;

import java.util.*;

import static pt.fct.nova.id.srv.application.Utils.extractVariablesPattern;
import static pt.fct.nova.id.srv.application.Utils.generateID;

public class SPARQLPlanner extends OpVisitorByTypeBase {

    private final Map<Op, String> parsed_op;

    private final Deque<Job> plan;

    public SPARQLPlanner() {
        this.parsed_op = new HashMap<>();
        this.plan = new LinkedList<>();
    }

    void opVisitorWalker(Op op) {
        OpWalker.walk(op, this);
    }

    public Queue<Job> getPlan() {
        return plan;
    }

    @Override
    public void visit0(Op0 op) throws NotImplemented {
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
                    String jobID = parsed_op.get(op);
                    if (jobID == null) {
                        jobID = generateID();
                        parsed_op.put(op, jobID);
                        plan.addFirst(new GetJob(jobID, extractVariablesPattern(s, p, o), s, p, o));
                    }
                }
        );
    }

    private void generateValuesJob(OpTable op) {
        Set<Binding> values = new LinkedHashSet<>();
        op.getTable().rows().forEachRemaining(values::add);
        String jobID = parsed_op.get(op);
        if (jobID == null) {
            jobID = generateID();
            parsed_op.put(op, jobID);
            plan.addFirst(new ValuesJob(generateID(), values));
        }
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
        op.getVarExprList().getExprs().forEach(
                (var, expr) -> plan.addFirst(new BindJob(generateID(), var, expr))
        );
    }

    private void generateFilterJob(OpFilter op) {
        //TODO: Generate FilterJobs
    }


    private void generateGroupJob(OpGroup op) {
        //TODO: Generate GroupJobs
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
        //TODO: Generate ProjectJobs
    }

    private void generateDistinctJob(OpDistinctReduced op) {
        //TODO: Generate DistinctJobs
    }

    private void generateOrderByJob(OpOrder op) {
        //TODO: Generate OrderByJobs
    }

    private void generateSliceJob(OpSlice op) {
        //TODO: Generate SliceJobs
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
        //TODO: Generate JoinJob
    }

    private void generateOptionalJob(OpLeftJoin op) {
        //TODO: Generate OptionalJob
    }

    private void generateMinusJob(OpMinus op) {
        //TODO: Generate MinusJob
    }

    private void generateUnionJob(OpUnion op) {
        //TODO: Generate UnionJob
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
