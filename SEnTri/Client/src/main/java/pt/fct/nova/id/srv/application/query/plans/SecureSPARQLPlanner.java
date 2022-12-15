package pt.fct.nova.id.srv.application.query.plans;

import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.QueryBuildException;
import org.apache.jena.query.SortCondition;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpVisitorByType;
import org.apache.jena.sparql.algebra.OpWalker;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.fct.nova.id.srv.application.query.jobs.*;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.*;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.JoinJob;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.MinusJob;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.OptionalJob;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.UnionJob;

import java.util.*;

import static pt.fct.nova.id.srv.application.query.Utils.extractVariablesPattern;
import static pt.fct.nova.id.srv.application.query.Utils.generateID;

public class SecureSPARQLPlanner extends OpVisitorByType implements SPARQLPlanner {
    final static Logger logger = LoggerFactory.getLogger(DefaultSPARQLPlanner.class);

    private final Map<Op, String> parsed_op;

    private final DefaultQueryExecutionPlan plan;

    public SecureSPARQLPlanner() {
        this.parsed_op = new HashMap<>();
        this.plan = new DefaultQueryExecutionPlan();
    }

    public QueryExecutionPlan generatePlan(Op op, List<String> resultVarNames) {
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
        logger.info("OP0: {}", op);
        if (op instanceof OpBGP opBGP) {
            generateGetJobs(opBGP);
        } else if (op instanceof OpTriple opTriple) {
            generateGetJobs(opTriple.asBGP());
        } else if (op instanceof OpTable opTable) {
            generateValuesJob(opTable);
        } else {
            throw new NotImplemented();
        }
    }

    private void generateGetJobs(OpBGP op) {
        List<Triple> patterns = op.getPattern().getList();
        int total_patterns = patterns.size();
        List<SearchJob> searchJobs = new ArrayList<>(total_patterns);

        patterns.forEach(
                t -> {
                    Node s = t.getSubject();
                    Node p = t.getPredicate();
                    Node o = t.getObject();
                    String jobID = generateID();
                    if (total_patterns == 1) {
                        parsed_op.put(op, jobID);
                        plan.pushJob(new SearchJob(jobID, extractVariablesPattern(s, p, o), s, p, o));
                    } else
                        searchJobs.add(new SearchJob(jobID, extractVariablesPattern(s, p, o), s, p, o));
                }
        );
        if (total_patterns >= 2)
            generateJoinPipeline(op, searchJobs);
    }

    private void generateJoinPipeline(OpBGP op, List<SearchJob> searchJobs) {
        int num_jobs = searchJobs.size();
        Set<Var> all_vars = new HashSet<>();
        List<Set<Var>> result_vars = new ArrayList<>(num_jobs * 2);
        List<Job> joins = new ArrayList<>(num_jobs);
        List<Job> pipeline = new ArrayList<>(num_jobs * 2);
        Set<Integer> to_be_processed = new HashSet<>();

        Set<Var> vars;
        for (int i = 0; i < num_jobs; i++) {
            vars = extractVars(searchJobs.get(i));
            all_vars.addAll(vars);
            result_vars.add(vars);
            to_be_processed.add(i);
        }

        int current, last = num_jobs, compatible = -1;
        boolean stop;
        Job l, r, join;
        Set<Var> vars2, resVars;
        while (!to_be_processed.isEmpty()) {
            stop = false;
            current = to_be_processed.iterator().next();
            for (Integer i : to_be_processed) {
                if (current != i) {
                    vars = result_vars.get(current);
                    vars2 = result_vars.get(i);
                    resVars = new HashSet<>(vars2);
                    for (Var v : vars) {
                        if (!vars2.isEmpty() && vars2.contains(v)) {
                            compatible = i;
                            if (current < num_jobs) l = searchJobs.get(current);
                            else l = joins.get(current - num_jobs);
                            if (i < num_jobs) r = searchJobs.get(i);
                            else r = joins.get(i - num_jobs);
                            pipeline.add(l);
                            pipeline.add(r);
                            join = new JoinJob(generateID(), l.getID(), r.getID());
                            pipeline.add(join);
                            resVars.addAll(vars);
                            joins.add(last - num_jobs, join);
                            result_vars.add(last, resVars);
                            vars2.remove(v);
                            stop = true;
                            break;
                        }
                    }
                }
                if (stop) break;
            }
            if (compatible < 0) {
                String jobID = generateID();
                plan.pushJob(new EmptyResJob(jobID, all_vars));
                parsed_op.put(op, jobID);
                return;
            }
            to_be_processed.remove(current);
            to_be_processed.remove(compatible);
            if (!to_be_processed.isEmpty())
                to_be_processed.add(last);
            last++;
            compatible = -1;
        }
        if (!pipeline.isEmpty()) {
            for (Job j : pipeline)
                plan.pushJob(j);
            parsed_op.put(op, pipeline.get(pipeline.size() - 1).getID());
        }
    }

    private Set<Var> extractVars(SearchJob job) {
        Node s = job.getSubject();
        Node p = job.getPredicate();
        Node o = job.getObject();
        Set<Var> res = new HashSet<>();
        switch (job.getVariablesPattern()) {
            case S -> res.add(Var.alloc(s));
            case P -> res.add(Var.alloc(p));
            case O -> res.add(Var.alloc(o));
            case SP -> {
                res.add(Var.alloc(s));
                res.add(Var.alloc(p));
            }
            case SO -> {
                res.add(Var.alloc(s));
                res.add(Var.alloc(o));
            }
            case PO -> {
                res.add(Var.alloc(p));
                res.add(Var.alloc(o));
            }
            case SPO -> {
                res.add(Var.alloc(s));
                res.add(Var.alloc(p));
                res.add(Var.alloc(o));
            }
        }
        return res;
    }

    private void generateValuesJob(OpTable op) {
        List<SerializableBinding> values = new LinkedList<>();
        Iterator<Binding> rows = op.getTable().rows();
        Binding row;
        Iterator<Var> rowVars;
        Var currentVar;
        Map<Var, Node> collector;
        while (rows.hasNext()) {
            row = rows.next();
            rowVars = row.vars();
            collector = new HashMap<>();
            while (rowVars.hasNext()) {
                currentVar = rowVars.next();
                collector.put(currentVar, row.get(currentVar));
            }
            values.add(new SerializableBinding(collector));

        }
        String jobID = generateID();
        parsed_op.put(op, jobID);
        plan.pushJob(new ValuesJob(jobID, values));
    }

    @Override
    public void visit1(Op1 op) {
        logger.info("OP1: {}", op);
        if (op instanceof OpExtendAssign) {
            throw new NotImplemented();
        } else if (op instanceof OpFilter) {
            //generateBindJob((OpExtendAssign) op);
            throw new NotImplemented();
        } else if (op instanceof OpGroup) {
            //generateFilterJob((OpFilter) op);
            throw new NotImplemented();
        } else if (op instanceof OpModifier opModifier) {
            //generateGroupJob((OpGroup) op);
            visitOpModifier(opModifier);
        } else {
            throw new NotImplemented();
        }
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
        if (op instanceof OpDistinctReduced opDistinctReduced) {
            generateDistinctJob(opDistinctReduced);
        } else if (op instanceof OpOrder opOrder) {
            generateOrderByJob(opOrder);
        } else if (op instanceof OpProject opProject) {
            generateProjectJob(opProject);
        } else if (op instanceof OpSlice opSlice) {
            generateSliceJob(opSlice);
        } else {
            throw new NotImplemented();
        }
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
        String jobID = generateID();
        List<SortCondition> conditions = op.getConditions();
        List<SerializableSortCondition> serializableSortConditions = new ArrayList<>(conditions.size());
        for (SortCondition sortCondition : conditions)
            serializableSortConditions.add(new SerializableSortCondition(sortCondition.getExpression().asVar(), sortCondition.getDirection()));
        plan.pushJob(new OrderByJob(jobID, getPrevJobID(op.getSubOp()), serializableSortConditions));
        parsed_op.put(op, jobID);
    }

    private void generateSliceJob(OpSlice op) {
        String jobID = generateID();
        plan.pushJob(new SliceJob(jobID, getPrevJobID(op.getSubOp()), op.getStart(), op.getLength()));
        parsed_op.put(op, jobID);
    }


    @Override
    public void visit2(Op2 op) {
        logger.info("OP2: {}", op);
        if (op instanceof OpJoin opJoin) {
            generateJoinJob(opJoin);
        } else if (op instanceof OpLeftJoin opLeftJoin) {
            generateOptionalJob(opLeftJoin);
        } else if (op instanceof OpMinus opMinus) {
            generateMinusJob(opMinus);
        } else if (op instanceof OpUnion opUnion) {
            generateUnionJob(opUnion);
        } else {
            throw new NotImplemented();
        }
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
    protected void visitFilter(OpFilter opFilter) {
        this.visit1(opFilter);
    }

    @Override
    protected void visitLeftJoin(OpLeftJoin opLeftJoin) {
        this.visit2(opLeftJoin);
    }

    @Override
    public void visitN(OpN op) {
        throw new NotImplemented();
    }
}
