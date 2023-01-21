package pt.fct.nova.id.srv.application.query.plans;

import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.QueryBuildException;
import org.apache.jena.query.SortCondition;
import org.apache.jena.sparql.algebra.AlgebraGenerator;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpVisitorByType;
import org.apache.jena.sparql.algebra.OpWalker;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.modify.request.UpdateDataDelete;
import org.apache.jena.sparql.modify.request.UpdateDataInsert;
import org.apache.jena.sparql.modify.request.UpdateDeleteWhere;
import org.apache.jena.sparql.modify.request.UpdateModify;
import org.apache.jena.update.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.fct.nova.id.srv.application.protocols.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.query.QueryType;
import pt.fct.nova.id.srv.application.query.jobs.*;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.*;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.JoinJob;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.MinusJob;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.OptionalJob;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.UnionJob;
import pt.fct.nova.id.srv.presentation.controllers.ParsingUtils;

import java.util.*;

import static pt.fct.nova.id.srv.application.query.QueryUtils.*;
import static pt.fct.nova.id.srv.application.query.QueryUtils.generateID;
import static pt.fct.nova.id.srv.application.query.jobs.VariablesPattern.*;

public class SecureSPARQLPlanner extends OpVisitorByType implements SPARQLPlanner {

    final static Logger logger = LoggerFactory.getLogger(DefaultSPARQLPlanner.class);
    private final Map<Op, String> parsed_op;
    private final DefaultQueryExecutionPlan plan;
    private final HashMap<Var, Var> obfuscationMap;
    private final Set<String> keywords;
    private final Set<String> searchJobsIDs;
    private final Random rnd;
    private QueryType queryType;
    private List<Triple> constructTemplate;
    private final List<Triple> uploadTemplate;
    private final List<Triple> deleteTemplate;

    public SecureSPARQLPlanner() {
        this.keywords = new HashSet<>();
        this.parsed_op = new HashMap<>();
        this.obfuscationMap = new HashMap<>();
        this.plan = new DefaultQueryExecutionPlan();
        this.searchJobsIDs = new HashSet<>();
        this.rnd = new Random();
        this.uploadTemplate = new LinkedList<>();
        this.deleteTemplate = new LinkedList<>();
    }
    @Override
    public QueryExecutionPlan generatePlan(Op op) {
        OpWalker.walk(op, this);
        return plan;
    }

    @Override
    public QueryExecutionPlan generatePlan(Update update, AlgebraGenerator algebraGenerator) throws NotImplemented {
        if (update instanceof UpdateDataInsert op) {
            visitUpdate(op);
        } else if (update instanceof UpdateDataDelete op) {
            visitUpdate(op);
        } else if (update instanceof UpdateModify op) {
            visitUpdate(op, algebraGenerator);
        } else if (update instanceof UpdateDeleteWhere op) {
            visitUpdate(op);
        } else {
            throw new NotImplemented();
        }
        return plan;
    }

    @Override
    public QueryType getQueryType() {
        return queryType;
    }

    @Override
    public void setQueryType(QueryType queryType) {
        this.queryType = queryType;
    }

    @Override
    public void setConstructTemplate(List<Triple> constructTemplate) {
        if (queryType.equals(QueryType.CONSTRUCT))
            this.constructTemplate = constructTemplate;
    }

    @Override
    public List<Triple> getConstructTemplate() {
        if (queryType.equals(QueryType.CONSTRUCT))
            return constructTemplate;
        else throw new QueryBuildException();
    }

    @Override
    public List<Triple> getUploadTemplate() {
        return uploadTemplate;
    }

    @Override
    public List<Triple> getDeleteTemplate() {
        return deleteTemplate;
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

    private Var obfuscateVar(Var var) {
        Var obfuscatedVar = obfuscationMap.get(var);
        if (obfuscatedVar == null) {
            obfuscatedVar = Var.alloc(generateID());
            System.out.println("Obfuscation: " + var + " | " + obfuscatedVar);
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
            } else if (op instanceof OpTable) {
                throw new NotImplemented();
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
        List<SecureSearchJob> searchJobs = new ArrayList<>(2 * total_patterns);
        Node s, p, o;
        for (Triple t : patterns) {
            s = t.getSubject();
            p = t.getPredicate();
            o = t.getObject();
            switch (extractVariablesPattern(s, p, o)) {
                case S -> generateSecureSearchJob(searchJobs, Var.alloc(s), p, o, S);
                case P -> generateSecureSearchJob(searchJobs, Var.alloc(p), s, o, P);
                case O -> generateSecureSearchJob(searchJobs, Var.alloc(o), s, p, O);
                case SP -> generateSecureSearchJob(searchJobs, Var.alloc(s), Var.alloc(p), o, SP);
                case SO -> generateSecureSearchJob(searchJobs, Var.alloc(s), Var.alloc(o), p, SO);
                case PO -> generateSecureSearchJob(searchJobs, Var.alloc(p), Var.alloc(o), s, PO);
            }
        }
        if (searchJobs.size() > 1)
            generateRandomJoinPipeline(op, searchJobs);
        else {
            parsed_op.put(op, searchJobs.get(0).getID());
            plan.pushJob(searchJobs.get(0));
        }
    }

    private void generateSecureSearchJob(List<SecureSearchJob> secureSearchJobs, Var var, Node node2, Node node3, VariablesPattern pattern) throws InvalidNodeException {
        Map<Var, String> searches = new HashMap<>();
        var = obfuscateVar(var);
        String jobID = generateID();
        String keyword = ParsingUtils.generateKeyword(pattern, ParsingUtils.parseKeyword(node2), ParsingUtils.parseKeyword(node3));
        searches.put(var, keyword);
        keywords.add(keyword);
        searchJobsIDs.add(jobID);
        secureSearchJobs.add(new SecureSearchJob(jobID, new Var[]{var}, searches));
    }

    private void generateSecureSearchJob(List<SecureSearchJob> secureSearchJobs, Var var1, Var var2, Node node, VariablesPattern pattern) throws InvalidNodeException {
        Map<Var, String> searches = new HashMap<>();
        String jobID = generateID();
        String keyword = ParsingUtils.generateKeyword(pattern, ParsingUtils.parseKeyword(node));
        keywords.add(keyword);
        searchJobsIDs.add(jobID);
        var1 = obfuscateVar(var1);
        var2 = obfuscateVar(var2);
        searches.put(var1, keyword);
        searches.put(var2, keyword);
        secureSearchJobs.add(new SecureSearchJob(jobID, new Var[]{var1, var2}, searches));
    }


    private void generateRandomJoinPipeline(OpBGP op, List<SecureSearchJob> searchJobs) {
        int numJobs = searchJobs.size();
        Map<Integer, List<Integer>> graph = new HashMap<>(numJobs);
        List<Integer> jobs = new ArrayList<>(numJobs);
        Set<Var> allVars = new HashSet<>();
        generateAdjacencyMatrix(searchJobs, numJobs, graph, jobs, allVars);

        List<Integer> walk = new ArrayList<>(numJobs);
        Set<Integer> sampled = new HashSet<>();
        int next;
        boolean stop = false;
        System.out.println("Total jobs:" + numJobs);
        while (sampled.size() < numJobs && !stop) {
            next = rndSample(jobs, sampled);
            sampled.add(next);
            bfsSearch(next, graph, walk, numJobs);
            if (walk.size() == numJobs)
                stop = true;
        }
        System.out.println(Arrays.toString(walk.toArray()));
        if (walk.size() == numJobs) {
            SecureSearchJob job1, job2;
            List<JoinJob> joins = new LinkedList<>();
            int j1, j2;
            String joinID;
            for (int i = 0; i < numJobs - 1; i += 2) {
                j1 = walk.get(i);
                j2 = walk.get(i + 1);
                job1 = searchJobs.get(j1);
                job2 = searchJobs.get(j2);
                plan.pushJob(job1);
                plan.pushJob(job2);
                joinID = generateID();
                joins.add(new JoinJob(joinID, job1.getID(), job2.getID()));
                System.out.println("[" + joinID + "] - (" + j1 + "," + j2 + ")");
            }
            if (numJobs % 2 == 1) {
                job1 = searchJobs.get(walk.get(searchJobs.size() - 1));
                JoinJob join = joins.remove(joins.size() - 1);
                plan.pushJob(job1);
                plan.pushJob(join);
                joinID = generateID();
                joins.add(new JoinJob(joinID, job1.getID(), join.getID()));
                System.out.println("[" + joinID + "] - (" + walk.get(searchJobs.size() - 1) + "," + join.getID() + ")");
            }
            JoinJob join1, join2;
            while (joins.size() > 1) {
                join1 = joins.remove(0);
                join2 = joins.remove(0);
                plan.pushJob(join1);
                plan.pushJob(join2);
                joinID = generateID();
                joins.add(new JoinJob(joinID, join1.getID(), join2.getID()));
                System.out.println("[" + joinID + "] - (" + join1.getID() + "," + join2.getID() + ")");
            }
            plan.pushJob(joins.get(0));
            parsed_op.put(op, joins.get(0).getID());
        } else {
            String jobID = generateID();
            plan.pushJob(new EmptyResJob(jobID, allVars));
            parsed_op.put(op, jobID);
        }
    }

    private static void generateAdjacencyMatrix(List<SecureSearchJob> searchJobs, int numJobs, Map<Integer, List<Integer>> graph, List<Integer> jobs, Set<Var> allVars) {
        SecureSearchJob job1, job2;
        List<Integer> edges;
        Set<Var> v2, v1;
        for (int i = 0; i < numJobs; i++) {
            job1 = searchJobs.get(i);
            jobs.add(i);
            v1 = job1.getSearches().keySet();
            allVars.addAll(v1);
            System.out.println("[" + i + "] - " + Arrays.toString(v1.toArray()));
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
        graph.forEach((j, l) -> System.out.println("[" + j + "] - " + Arrays.toString(l.toArray())));
    }

    private void bfsSearch(int root, Map<Integer, List<Integer>> adjacencyMatrix, List<Integer> path, int depth) {
        Queue<Integer> queue = new LinkedList<>();
        path.add(root);
        queue.add(root);
        int next;
        while (path.size() < depth) {
            next = queue.poll();
            for (int neighbour : adjacencyMatrix.get(next)) {
                if (!path.contains(neighbour)) {
                    path.add(neighbour);
                    queue.add(neighbour);
                }
            }
            if (queue.isEmpty())
                return;
        }
    }

    private int rndSample(List<Integer> values, Set<Integer> exclude) {
        int i;
        do {
            i = rnd.nextInt(values.size());
        } while (exclude.contains(i));
        return values.get(i);
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
        plan.setVars(obfuscatedVars);
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

    public void visitUpdate(UpdateDataInsert op) {
        setQueryType(QueryType.INSERT_DATA);
        op.getQuads().forEach(quad -> uploadTemplate.add(quad.asTriple()));
    }

    public void visitUpdate(UpdateDataDelete op) {
        setQueryType(QueryType.DELETE_DATA);
        op.getQuads().forEach(quad -> deleteTemplate.add(quad.asTriple()));
    }

    public void visitUpdate(UpdateDeleteWhere op) {
        setQueryType(QueryType.MODIFY);
        List<Triple> bgp = new LinkedList<>();
        op.getQuads().forEach(quad -> deleteTemplate.add(quad.asTriple()));
        OpWalker.walk(new OpBGP(BasicPattern.wrap(bgp)), this);
    }

    public void visitUpdate(UpdateModify op, AlgebraGenerator algebraGenerator) {
        setQueryType(QueryType.MODIFY);
        if (op.hasInsertClause())
            op.getInsertQuads().forEach(quad -> uploadTemplate.add(quad.asTriple()));
        if (op.hasDeleteClause())
            op.getDeleteQuads().forEach(quad -> deleteTemplate.add(quad.asTriple()));
        OpWalker.walk(algebraGenerator.compile(op.getWherePattern()), this);
    }
}
