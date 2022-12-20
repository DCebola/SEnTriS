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
import pt.fct.nova.id.srv.application.protocols.EncryptionProtocol;
import pt.fct.nova.id.srv.application.protocols.Protocol1;
import pt.fct.nova.id.srv.application.protocols.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.query.jobs.*;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.*;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.JoinJob;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.MinusJob;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.OptionalJob;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.UnionJob;
import pt.fct.nova.id.srv.presentation.controllers.ParsingUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static pt.fct.nova.id.srv.application.query.Utils.*;
import static pt.fct.nova.id.srv.application.query.Utils.generateID;

public class SecureSPARQLPlanner extends OpVisitorByType implements SPARQLPlanner {

    final static Logger logger = LoggerFactory.getLogger(DefaultSPARQLPlanner.class);
    private final Map<Op, String> parsed_op;
    private final DefaultQueryExecutionPlan plan;
    private final HashMap<Var, Var> obfuscationMap;
    private final Set<String> keywords;
    private final EncryptionProtocol protocol;
    private final Set<String> searchJobsIDs;
    private Random rnd;

    public SecureSPARQLPlanner(EncryptionProtocol protocol) {
        this.protocol = protocol;
        this.keywords = new HashSet<>();
        this.parsed_op = new HashMap<>();
        this.obfuscationMap = new HashMap<>();
        this.plan = new DefaultQueryExecutionPlan();
        this.searchJobsIDs = new HashSet<>();
        this.rnd = new Random();
    }

    public QueryExecutionPlan generatePlan(Op op, List<String> resultVarNames) {
        OpWalker.walk(op, this);
        List<Var> vars = generateVars(resultVarNames);
        List<Var> obfuscatedVars = new ArrayList<>(vars.size());
        for (Var var : vars)
            obfuscatedVars.add(obfuscateVar(var));
        plan.setVars(obfuscatedVars);
        return plan;
    }

    public Set<String> getKeywords() {
        return keywords;
    }

    public Set<String> getSearchJobsIDs() {
        return searchJobsIDs;
    }

    public Map<Var, Var> getObfuscationMap() {
        return obfuscationMap;
    }

    private List<Var> generateVars(List<String> resultVarNames) {
        List<Var> vars = new LinkedList<>();
        resultVarNames.forEach(v -> vars.add(Var.alloc(v)));
        return vars;
    }

    private Var obfuscateVar(Var var) {
        Var obfuscatedVar = obfuscationMap.get(var);
        if (obfuscatedVar == null) {
            obfuscatedVar = Var.alloc(generateID());
            obfuscationMap.put(var, obfuscatedVar);
            obfuscationMap.put(obfuscatedVar, var);
        }
        return obfuscatedVar;
    }

    @Override
    public void visit0(Op0 op) {
        logger.info("OP0: {}", op);
        try {
            if (op instanceof OpBGP opBGP) {
                generateGetJobs(opBGP);
            } else if (op instanceof OpTriple opTriple) {
                generateGetJobs(opTriple.asBGP());
            } else if (op instanceof OpTable opTable) {
                generateValuesJob(opTable);
            } else {
                throw new NotImplemented();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void generateGetJobs(OpBGP op) throws InvalidNodeException {
        List<Triple> patterns = op.getPattern().getList();
        int total_patterns = patterns.size();
        List<SecureSearchJob> searchJobs = new ArrayList<>(total_patterns);
        Node s, p, o;
        Var var;
        String jobID, keyword;
        Map<Var, String> searches;
        Map<Var, String> obfuscatedSearches;
        for (Triple t : patterns) {
            s = t.getSubject();
            p = t.getPredicate();
            o = t.getObject();
            jobID = generateID();
            searches = generateKeywordMap(s, p, o, extractVariablesPattern(s, p, o));
            obfuscatedSearches = new HashMap<>();
            for (Map.Entry<Var, String> entry : searches.entrySet()) {
                var = entry.getKey();
                keyword = entry.getValue();
                obfuscatedSearches.put(obfuscateVar(var), keyword);
                keywords.add(keyword);
            }
            searchJobsIDs.add(jobID);
            if (total_patterns == 1) {
                parsed_op.put(op, jobID);
                plan.pushJob(new SecureSearchJob(jobID, obfuscatedSearches));
            } else
                searchJobs.add(new SecureSearchJob(jobID, obfuscatedSearches));
        }
        if (total_patterns >= 2)
            generateRandomJoinPipeline(op, searchJobs);
    }


    private void generateRandomJoinPipeline(OpBGP op, List<SecureSearchJob> searchJobs) {
        int numJobs = searchJobs.size();
        Map<Integer, List<Integer>> graph = new HashMap<>(numJobs);
        List<Integer> edges, jobs = new ArrayList<>(numJobs);
        Set<Var> v1, v2, allVars = new HashSet<>();
        SecureSearchJob job1, job2;
        for (int i = 0; i < numJobs; i++) {
            job1 = searchJobs.get(i);
            jobs.add(i);
            v1 = job1.getSearches().keySet();
            allVars.addAll(v1);
            edges = new ArrayList<>(numJobs);
            for (int j = 0; j < numJobs; j++) {
                job2 = searchJobs.get(j);
                v2 = job2.getSearches().keySet();
                if (i != j) {
                    for (Var v : v1) {
                        if (v2.contains(v)) {
                            edges.add(j);
                            break;
                        }
                    }
                }
            }
            graph.put(i, edges);
        }

        List<Integer> walk = new ArrayList<>(numJobs);
        Set<Integer> sampled = new HashSet<>();
        int next;
        while (sampled.size() < numJobs) {
            next = rndSample(jobs, sampled);
            sampled.add(next);
            randomWalk(next, graph, walk, numJobs);
            if (walk.size() == numJobs)
                break;
        }
        if (walk.size() == numJobs) {
            List<Job> joins = new LinkedList<>();
            for (int i = 0; i < numJobs; i += 2) {
                job1 = searchJobs.get(i);
                job2 = searchJobs.get(i + 1);
                plan.pushJob(job1);
                plan.pushJob(job2);
                joins.add(new JoinJob(generateID(), job1.getID(), job2.getID()));
            }
            while (joins.size() > 1) {
                job1 = searchJobs.remove(0);
                job2 = searchJobs.remove(0);
                plan.pushJob(job1);
                plan.pushJob(job2);
                joins.add(new JoinJob(generateID(), job1.getID(), job2.getID()));
            }
            plan.pushJob(joins.get(0));
            parsed_op.put(op, joins.get(0).getID());
        } else {
            String jobID = generateID();
            plan.pushJob(new EmptyResJob(jobID, allVars));
            parsed_op.put(op, jobID);
        }
    }


    private void randomWalk(int root, Map<Integer, List<Integer>> adjacencyMatrix, List<Integer> visited, int depth) {
        if (visited.size() < depth) {
            List<Integer> neighbours = new ArrayList<>(adjacencyMatrix.get(root));
            neighbours.removeAll(visited);
            int next;
            Set<Integer> sampled = new HashSet<>();
            while (sampled.size() < neighbours.size()) {
                next = rndSample(neighbours, sampled);
                sampled.add(next);
                visited.add(next);
                randomWalk(next, adjacencyMatrix, visited, depth);
            }
        }
    }

    private int rndSample(List<Integer> values, Set<Integer> exclude) {
        int i;
        do {
            i = rnd.nextInt(values.size());
        } while (exclude.contains(i));
        return values.get(i);
    }

    private void generateJoinPipeline(OpBGP op, List<SecureSearchJob> searchJobs) {
        int num_jobs = searchJobs.size();
        Set<Var> all_vars = new HashSet<>();
        List<Set<Var>> result_vars = new ArrayList<>(num_jobs * 2);
        List<Job> joins = new ArrayList<>(num_jobs);
        List<Job> pipeline = new ArrayList<>(num_jobs * 2);
        Set<Integer> to_be_processed = new HashSet<>();

        Set<Var> vars;
        for (int i = 0; i < num_jobs; i++) {
            vars = searchJobs.get(i).getSearches().keySet();
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


    private void generateValuesJob(OpTable op) throws InvalidNodeException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        List<EncryptedBinding> values = new LinkedList<>();
        Iterator<Binding> rows = op.getTable().rows();
        Binding row;
        Iterator<Var> rowVars;
        Var currentVar;
        Map<Var, String> collector;
        while (rows.hasNext()) {
            row = rows.next();
            rowVars = row.vars();
            collector = new HashMap<>();
            while (rowVars.hasNext()) {
                currentVar = rowVars.next();
                if (protocol instanceof Protocol1 p1)
                    collector.put(obfuscateVar(currentVar),
                            new String(p1.encryptDET(ParsingUtils.parseNodeIRI(row.get(currentVar)).getBytes(StandardCharsets.UTF_8))));
                else
                    throw new NotImplemented();
            }
            values.add(new EncryptedBinding(collector));

        }
        String jobID = generateID();
        parsed_op.put(op, jobID);
        plan.pushJob(new EncryptedValuesJob(jobID, values));
    }

    @Override
    public void visit1(Op1 op) {
        // logger.info("OP1: {}", op);
        if (op instanceof OpExtendAssign) {
            throw new NotImplemented();
        } else if (op instanceof OpFilter) {
            throw new NotImplemented();
        } else if (op instanceof OpGroup) {
            throw new NotImplemented();
        } else if (op instanceof OpModifier opModifier) {
            visitOpModifier(opModifier);
        } else {
            throw new NotImplemented();
        }
    }

    private String getPrevJobID(Op subOp) {
        String prevJobID = parsed_op.get(subOp);
        if (prevJobID == null)
            throw new QueryBuildException();
        return prevJobID;
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
        List<Var> vars = op.getVars();
        List<Var> obfuscatedVars = new ArrayList<>(vars.size());
        for (Var var : vars)
            obfuscatedVars.add(obfuscateVar(var));
        plan.pushJob(new ProjectJob(jobID, getPrevJobID(op.getSubOp()), obfuscatedVars));
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
            serializableSortConditions.add(new SerializableSortCondition(obfuscateVar(sortCondition.getExpression().asVar()), sortCondition.getDirection()));
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
        //logger.info("OP2: {}", op);
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
