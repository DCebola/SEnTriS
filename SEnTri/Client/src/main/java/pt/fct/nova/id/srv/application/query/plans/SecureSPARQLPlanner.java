package pt.fct.nova.id.srv.application.query.plans;

import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
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

import static pt.fct.nova.id.srv.application.protocols.EncryptionProtocol.COMPOUND_KEYWORD;
import static pt.fct.nova.id.srv.application.protocols.EncryptionProtocol.KEYWORD_FORMAT;
import static pt.fct.nova.id.srv.application.query.Utils.*;
import static pt.fct.nova.id.srv.application.query.Utils.generateID;
import static pt.fct.nova.id.srv.application.query.jobs.VariablesPattern.*;

public class SecureSPARQLPlanner extends OpVisitorByType implements SPARQLPlanner {

    final static Logger logger = LoggerFactory.getLogger(DefaultSPARQLPlanner.class);
    private final Map<Op, String> parsed_op;
    private final DefaultQueryExecutionPlan plan;
    private final HashMap<Var, Var> obfuscationMap;
    private final Set<String> keywords;
    private final EncryptionProtocol protocol;
    private final Set<String> searchJobsIDs;
    private final Random rnd;

    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder();

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
            System.out.println("Obfuscation: "+ var + " | " + obfuscatedVar);
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
        String keyword = String.format(KEYWORD_FORMAT, pattern, String.format(COMPOUND_KEYWORD, ParsingUtils.parseKeyword(node2), ParsingUtils.parseKeyword(node3)));
        searches.put(var, keyword);
        keywords.add(keyword);
        searchJobsIDs.add(jobID);
        secureSearchJobs.add(new SecureSearchJob(jobID, new Var[]{var}, searches));
    }

    private void generateSecureSearchJob(List<SecureSearchJob> secureSearchJobs, Var var1, Var var2, Node node, VariablesPattern pattern) throws InvalidNodeException {
        Map<Var, String> searches = new HashMap<>();
        String nodeKeyword = ParsingUtils.parseKeyword(node);
        String jobID = generateID();
        String keyword = String.format(KEYWORD_FORMAT, pattern, nodeKeyword);
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
            randomWalk(next, graph, walk, numJobs);
            if (walk.size() == numJobs)
                stop = true;
        }
        walk = new ArrayList<>(numJobs);
        walk.add(0);
        walk.add(1);
        walk.add(2);
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


    private void randomWalk(int root, Map<Integer, List<Integer>> adjacencyMatrix, List<Integer> visited, int depth) {
        System.out.println("[" + root + " ," + depth + " ]" + Arrays.toString(visited.toArray()));
        if (visited.size() < depth && !visited.contains(root)) {
            visited.add(root);
            List<Integer> neighbours = new ArrayList<>(adjacencyMatrix.get(root));
            neighbours.removeAll(visited);
            int next;
            Set<Integer> sampled = new HashSet<>();
            while (!visited.containsAll(neighbours)) {
                next = rndSample(neighbours, sampled);
                sampled.add(next);
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

    private void generateValuesJob(OpTable op) throws InvalidNodeException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        List<SerializableBinding> values = new LinkedList<>();
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
                if (protocol instanceof Protocol1 p1) {
                    String iri = ParsingUtils.parseNodeIRI(row.get(currentVar));
                    String value = base64Encoder.encodeToString(p1.encryptDET(iri.getBytes(StandardCharsets.UTF_8)));
                    System.out.println(iri + " | " + value);
                    collector.put(obfuscateVar(currentVar), value);
                } else
                    throw new NotImplemented();
            }
            values.add(new SerializableBinding(collector));

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
