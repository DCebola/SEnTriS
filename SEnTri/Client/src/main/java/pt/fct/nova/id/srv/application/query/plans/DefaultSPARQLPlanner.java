package pt.fct.nova.id.srv.application.query.plans;

import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.Restriction;
import org.apache.jena.query.QueryBuildException;
import org.apache.jena.query.SortCondition;
import org.apache.jena.sparql.algebra.AlgebraGenerator;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpVisitorByType;
import org.apache.jena.sparql.algebra.OpWalker;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.modify.request.UpdateDataDelete;
import org.apache.jena.sparql.modify.request.UpdateDataInsert;
import org.apache.jena.sparql.modify.request.UpdateDeleteWhere;
import org.apache.jena.sparql.modify.request.UpdateModify;
import org.apache.jena.update.Update;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.fct.nova.id.srv.application.ontologies.Ontology;
import pt.fct.nova.id.srv.application.protocols.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.query.QueryType;
import pt.fct.nova.id.srv.application.query.jobs.*;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.DistinctJob;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.OrderByJob;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.ProjectJob;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.SliceJob;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.JoinJob;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.MinusJob;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.OptionalJob;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.UnionJob;
import pt.fct.nova.id.srv.presentation.controllers.ParsingUtils;

import java.util.*;

import static pt.fct.nova.id.srv.application.query.QueryUtils.*;

public class DefaultSPARQLPlanner extends OpVisitorByType implements SPARQLPlanner {

    final static Logger logger = LoggerFactory.getLogger(DefaultSPARQLPlanner.class);
    private final Map<Op, String> parsed_op;
    private final DefaultQueryExecutionPlan plan;
    private final Random rnd;
    private final Set<Triple> uploadTemplate;
    private final Set<Triple> deleteTemplate;
    private QueryType queryType;
    private List<Triple> constructTemplate;

    private final Ontology ontology;

    public DefaultSPARQLPlanner() {
        this.parsed_op = new HashMap<>();
        this.plan = new DefaultQueryExecutionPlan();
        this.rnd = new Random();
        this.constructTemplate = new LinkedList<>();
        this.uploadTemplate = new HashSet<>();
        this.deleteTemplate = new HashSet<>();
        this.ontology = null;
    }

    public DefaultSPARQLPlanner(Ontology ontology) {
        this.parsed_op = new HashMap<>();
        this.plan = new DefaultQueryExecutionPlan();
        this.rnd = new Random();
        this.constructTemplate = new LinkedList<>();
        this.uploadTemplate = new HashSet<>();
        this.deleteTemplate = new HashSet<>();
        this.ontology = ontology;
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
    public Set<Triple> getUploadTemplate() {
        return uploadTemplate;
    }

    @Override
    public Set<Triple> getDeleteTemplate() {
        return deleteTemplate;
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
    public void visit0(Op0 op) {
        try {
            //logger.info("OP0: {}", op);
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
            throw new RuntimeException(e.getMessage());
        }
    }

    private void generateGetJobs(OpBGP op) throws InvalidNodeException {
        List<Triple> patterns = op.getPattern().getList();
        List<String> searchJobsIDs = new LinkedList<>();
        List<String> unionJobsIDs = new LinkedList<>();
        List<String> joinJobsIDs = new LinkedList<>();
        Map<String, Job> jobs = new HashMap<>();
        Map<String, String> jobIDs = new HashMap<>();
        Map<String, Set<Var>> bgp = new HashMap<>();
        Map<String, Set<Var>> jobsVars = new HashMap<>();
        Node s, p, o;
        String jobID = null;
        for (Triple t : patterns) {
            s = t.getSubject();
            p = t.getPredicate();
            o = t.getObject();
            jobID = pushSearch(s, p, o, searchJobsIDs, jobs, jobIDs, jobsVars);
            if (ontology != null) {
                if (p.equals(RDF.type.asNode()) && !o.isVariable())
                    jobID = expandClass("PATTERN", jobID, s, o, 0, searchJobsIDs, unionJobsIDs, joinJobsIDs, jobs, jobIDs, jobsVars);
                else if (!p.isVariable())
                    jobID = expandProperty("PATTERN", jobID, s, p, o, 0, searchJobsIDs, unionJobsIDs, joinJobsIDs, jobs, jobIDs, jobsVars, true);
            }
            bgp.put(jobID, jobsVars.get(jobID));
        }
        for (String searchJobID : searchJobsIDs) plan.pushJob(jobs.get(searchJobID));
        for (String joinJobID : joinJobsIDs) plan.pushJob(jobs.get(joinJobID));
        for (String unionJobID : unionJobsIDs) plan.pushJob(jobs.get(unionJobID));
        jobsVars.forEach((k, v) -> System.out.println("(" + k + "," + Arrays.toString(v.toArray()) + ")"));
        if (patterns.size() > 1) {
            generateRandomJoinPipeline(op, bgp);
        } else if (jobID != null)
            parsed_op.put(op, jobID);
    }

    private String pushSearch(Node s, Node p, Node o, List<String> searchJobsIDs, Map<String, Job> jobs,
                              Map<String, String> jobIDs, Map<String, Set<Var>> jobsVars) throws InvalidNodeException {
        String jobSignature = ParsingUtils.parseTriple(s, p, o);
        String jobID = jobIDs.get(jobSignature);
        if (jobID == null) {
            jobID = generateID();
            SearchJob job = new SearchJob(jobID, extractVariablesPattern(s, p, o), s, p, o);
            jobs.put(jobID, job);
            jobsVars.put(jobID, extractVars(job));
            jobIDs.put(jobSignature, jobID);
        }
        System.out.println("[SEARCH, " + jobID + "] - " + s + " | " + p + " | " + o);
        searchJobsIDs.add(jobID);
        return jobID;
    }

    private String pushUnion(String left, String right, List<String> unionJobsIDs, Map<String, Job> jobs, Map<String, String> jobIDs,
                             Map<String, Set<Var>> jobsVars) {
        String jobSignature = left.concat(right);
        String jobID = jobIDs.get(jobSignature);
        if (jobID == null) {
            jobID = generateID();
            UnionJob job = new UnionJob(jobID, left, right);
            jobs.put(jobID, job);
            Set<Var> vars = new HashSet<>(jobsVars.get(left));
            vars.addAll(jobsVars.get(right));
            jobsVars.put(jobID, vars);
            jobIDs.put(jobSignature, jobID);
        }
        System.out.println("[UNION, " + jobID + "] - " + left + " | " + right);
        unionJobsIDs.add(jobID);
        return jobID;
    }

    private String pushJoin(String left, String right, List<String> joinJobsIDs, Map<String, Job> jobs, Map<String, String> jobIDs,
                            Map<String, Set<Var>> jobsVars) {
        String jobSignature = left.concat(right);
        String jobID = jobIDs.get(jobSignature);
        if (jobID == null) {
            jobID = generateID();
            JoinJob job = new JoinJob(jobID, left, right);
            jobs.put(jobID, job);
            Set<Var> vars = new HashSet<>(jobsVars.get(left));
            vars.addAll(jobsVars.get(right));
            jobsVars.put(jobID, vars);
            jobIDs.put(jobSignature, jobID);
        }
        System.out.println("[JOIN, " + jobID + "] - " + left + " | " + right);
        joinJobsIDs.add(jobID);
        return jobID;
    }

    private String expandClass(String prefix, String previousJobID, Node s, Node o, int depth, List<String> searchJobsIDs,
                               List<String> unionJobsIDs, List<String> joinJobsIDs, Map<String, Job> jobs, Map<String, String> jobIDs, Map<String, Set<Var>> jobsVars) throws InvalidNodeException {
        assert ontology != null;
        if (depth == ontology.getMaximumExpansionDepth())
            return previousJobID;
        Node rdfType = RDF.type.asNode();
        System.out.println("[" + prefix + "," + depth + "] - " + Triple.create(s, rdfType, o));
        Set<OntClass> intersection = ontology.getIntersection(o);
        if (!intersection.isEmpty()) {
            previousJobID = expandClasses("INTERSECTION", intersection,
                    previousJobID, s, depth, searchJobsIDs, unionJobsIDs, joinJobsIDs, jobs, jobIDs, jobsVars, rdfType);
        } else {
            previousJobID = expandRestriction("RESTRICTION", ontology.getRestriction(o),
                    previousJobID, s, depth, searchJobsIDs, unionJobsIDs, joinJobsIDs, jobs, jobIDs, jobsVars, rdfType);
        }
        previousJobID = expandClasses("SUBCLASS", ontology.getSubClasses(o),
                previousJobID, s, depth, searchJobsIDs, unionJobsIDs, joinJobsIDs, jobs, jobIDs, jobsVars, rdfType);
        previousJobID = expandClasses("EQUIVALENT", ontology.getEquivalentClasses(o),
                previousJobID, s, depth, searchJobsIDs, unionJobsIDs, joinJobsIDs, jobs, jobIDs, jobsVars, rdfType);
        previousJobID = expandClasses("INTERSECTION-OPERAND", ontology.getIntersectionWhereClassIsOperand(o),
                previousJobID, s, depth, searchJobsIDs, unionJobsIDs, joinJobsIDs, jobs, jobIDs, jobsVars, rdfType);
        return previousJobID;
    }

    private String expandRestriction(String prefix, Restriction restriction, String previousJobID, Node s, int depth, List<String> searchJobsIDs,
                                     List<String> unionJobsIDs, List<String> joinJobsIDs, Map<String, Job> jobs, Map<String, String> jobIDs, Map<String, Set<Var>> jobsVars, Node rdfType) throws InvalidNodeException {
        Var var;
        Node property;
        Node value;
        if (restriction != null && (restriction.isHasValueRestriction() || restriction.isSomeValuesFromRestriction())) {
            if (restriction.isHasValueRestriction()) {
                var = Var.alloc(restriction.getOnProperty().asNode().getLocalName());
                property = restriction.getOnProperty().asNode();
                value = restriction.asHasValueRestriction().getHasValue().asNode();
            } else {
                var = Var.alloc(restriction.asSomeValuesFromRestriction().getSomeValuesFrom().asNode().getLocalName());
                property = restriction.getOnProperty().asNode();
                value = restriction.asSomeValuesFromRestriction().getSomeValuesFrom().asNode();
            }

            String joinID = pushJoin(
                    pushSearch(s, property, var, searchJobsIDs, jobs, jobIDs, jobsVars),
                    pushSearch(var, rdfType, value, searchJobsIDs, jobs, jobIDs, jobsVars),
                    joinJobsIDs, jobs, jobIDs, jobsVars);
            previousJobID = pushUnion(previousJobID, joinID, unionJobsIDs, jobs, jobIDs, jobsVars);
            previousJobID = expandProperty(prefix, previousJobID, s, property, var, depth + 1, searchJobsIDs, unionJobsIDs, joinJobsIDs, jobs, jobIDs, jobsVars, true);
            previousJobID = expandClass(prefix, previousJobID, var, value, depth + 1, searchJobsIDs, unionJobsIDs, joinJobsIDs, jobs, jobIDs, jobsVars);
        }
        return previousJobID;
    }

    private String expandClasses(String prefix, Set<OntClass> ontClasses, String previousJobID, Node s, int depth, List<String> searchJobIDs,
                                 List<String> unionJobsIDs, List<String> joinJobIDs, Map<String, Job> jobs, Map<String, String> jobIDs, Map<String, Set<Var>> jobsVars, Node rdfType) throws InvalidNodeException {
        for (OntClass ontClass : ontClasses) {
            if (!ontClass.isRestriction()) {
                previousJobID = pushUnion(
                        previousJobID,
                        pushSearch(s, rdfType, ontClass.asNode(), searchJobIDs, jobs, jobIDs, jobsVars),
                        unionJobsIDs, jobs, jobIDs, jobsVars
                );
            }
            previousJobID = expandClass(prefix, previousJobID, s, ontClass.asNode(), depth + 1, searchJobIDs, unionJobsIDs, joinJobIDs, jobs, jobIDs, jobsVars);
        }
        return previousJobID;
    }

    private String expandProperty(String prefix, String previousJobID, Node s, Node p, Node o, int depth,
                                  List<String> searchJobIDs, List<String> unionJobIDs, List<String> joinJobIDs, Map<String, Job> jobs, Map<String, String> jobIDs,
                                  Map<String, Set<Var>> jobsVars, boolean canExpandInverseOf) throws InvalidNodeException {
        assert ontology != null;
        if (depth == ontology.getMaximumExpansionDepth())
            return previousJobID;

        System.out.println("[" + prefix + "," + depth + "] - " + Triple.create(s, p, o));
        if (ontology.isSymmetric(p)) {
            previousJobID = pushUnion(
                    previousJobID,
                    pushSearch(o, p, s, searchJobIDs, jobs, jobIDs, jobsVars),
                    unionJobIDs, jobs, jobIDs, jobsVars
            );
            previousJobID = expandProperty("SYMMETRIC", previousJobID, o, p, s, depth + 1,
                    searchJobIDs, unionJobIDs, joinJobIDs, jobs, jobIDs, jobsVars, canExpandInverseOf);
        }
        if (ontology.isTransitive(p)) {
            System.out.println("#####");
            Var var = Var.alloc(p.getLocalName().concat(Integer.toString(0)));
            String leftID = pushSearch(s, p, var, searchJobIDs, jobs, jobIDs, jobsVars);
            String joinID = pushJoin(leftID, pushSearch(var, p, o, searchJobIDs, jobs, jobIDs, jobsVars), joinJobIDs, jobs, jobIDs, jobsVars);
            previousJobID = pushUnion(previousJobID, joinID, unionJobIDs, jobs, jobIDs, jobsVars);
            Var nextVar;
            for (int i = 1; i < ontology.getTransitivityDepth(); i++) {
                for (int j = 1; j < i + 1; j++) {
                    if (j == i) {
                        nextVar = Var.alloc(p.getLocalName().concat(Integer.toString(j)));
                        leftID = pushJoin(leftID, pushSearch(var, p, nextVar, searchJobIDs, jobs, jobIDs, jobsVars), joinJobIDs, jobs, jobIDs, jobsVars);
                        var = nextVar;
                    }
                }
                joinID = pushJoin(leftID, pushSearch(var, p, o, searchJobIDs, jobs, jobIDs, jobsVars), joinJobIDs, jobs, jobIDs, jobsVars);
                previousJobID = pushUnion(previousJobID, joinID, unionJobIDs, jobs, jobIDs, jobsVars);
            }
            System.out.println("#####");
        }

        previousJobID = expandProperties("SUB-PROPERTY", ontology.getSubProperties(p),
                previousJobID, s, o, depth, searchJobIDs, unionJobIDs, joinJobIDs, jobs, jobIDs, jobsVars, canExpandInverseOf);
        previousJobID = expandProperties("EQUIVALENT-PROPERTY", ontology.getEquivalentProperties(p),
                previousJobID, s, o, depth, searchJobIDs, unionJobIDs, joinJobIDs, jobs, jobIDs, jobsVars, canExpandInverseOf);
        if (canExpandInverseOf)
            previousJobID = expandProperties("INVERSE", ontology.getInverseOf(p),
                    previousJobID, s, o, depth, searchJobIDs, unionJobIDs, joinJobIDs, jobs, jobIDs, jobsVars, false);
        return previousJobID;
    }

    private String expandProperties(String prefix, Set<? extends OntProperty> ontProperties, String previousJobID, Node s, Node o, int depth,
                                    List<String> searchJobIDs, List<String> unionJobIDs, List<String> joinJobIDS, Map<String, Job> jobs, Map<String, String> jobIDs, Map<String, Set<Var>> jobsVars, boolean canExpandInverseOf) throws InvalidNodeException {
        for (OntProperty ontProperty : ontProperties) {
            previousJobID = pushUnion(
                    previousJobID,
                    pushSearch(s, ontProperty.asNode(), s, searchJobIDs, jobs, jobIDs, jobsVars),
                    unionJobIDs, jobs, jobIDs, jobsVars
            );
            previousJobID = expandProperty(prefix, previousJobID, o, ontProperty.asNode(), s, depth + 1,
                    searchJobIDs, unionJobIDs, joinJobIDS, jobs, jobIDs, jobsVars, canExpandInverseOf);
        }
        return previousJobID;
    }


    private void generateRandomJoinPipeline(OpBGP op, Map<String, Set<Var>> bgp) {
        int numJobs = bgp.size();
        Map<String, List<String>> graph = new HashMap<>(numJobs);
        List<String> jobs = new ArrayList<>(numJobs);
        Set<Var> allVars = new HashSet<>();
        generateAdjacencyMatrix(bgp, numJobs, graph, jobs, allVars);

        List<String> path = new ArrayList<>(numJobs);
        Set<String> sampled = new HashSet<>();
        String next;
        boolean stop = false;
        System.out.println("Total jobs:" + numJobs);
        while (sampled.size() < numJobs && !stop) {
            next = rndSample(jobs, sampled);
            sampled.add(next);
            bfsSearch(next, graph, path, numJobs);
            if (path.size() == numJobs)
                stop = true;
        }
        System.out.println(Arrays.toString(path.toArray()));
        if (path.size() == numJobs) {
            List<JoinJob> joins = new LinkedList<>();
            String jobID1, jobID2;
            String joinID;
            for (int i = 0; i < numJobs - 1; i += 2) {
                jobID1 = path.get(i);
                jobID2 = path.get(i + 1);
                joinID = generateID();
                joins.add(new JoinJob(joinID, jobID1, jobID2));
                System.out.println("[" + joinID + "] - (" + jobID1 + "," + jobID2 + ")");
            }
            if (numJobs % 2 == 1) {
                jobID1 = path.get(bgp.size() - 1);
                JoinJob join = joins.remove(joins.size() - 1);
                plan.pushJob(join);
                joinID = generateID();
                joins.add(new JoinJob(joinID, jobID1, join.getID()));
                System.out.println("[" + joinID + "] - (" + jobID1 + "," + joinID + ")");
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


    private static void generateAdjacencyMatrix(Map<String, Set<Var>> bgp, int numJobs, Map<String, List<String>> graph, List<String> jobs, Set<Var> allVars) {
        List<String> edges;
        Set<Var> v2, v1;
        for (String jobID : bgp.keySet()) {
            jobs.add(jobID);
            v1 = bgp.get(jobID);
            allVars.addAll(v1);
            System.out.println("[" + jobID + "] - " + Arrays.toString(v1.toArray()));
            edges = new ArrayList<>(numJobs);
            for (String jobID2 : bgp.keySet()) {
                v2 = bgp.get(jobID2);
                if (!jobID.equals(jobID2)) {
                    for (Var v : v1) {
                        if (v2.contains(v)) {
                            edges.add(jobID2);
                            break;
                        }
                    }
                }
            }
            graph.put(jobID, edges);
        }
        graph.forEach((job, l) -> System.out.println("[" + job + "] - " + Arrays.toString(l.toArray())));
    }

    private void bfsSearch(String root, Map<String, List<String>> adjacencyMatrix, List<String> path, int depth) {
        Queue<String> queue = new LinkedList<>();
        path.add(root);
        queue.add(root);
        String next;
        while (path.size() < depth) {
            next = queue.poll();
            for (String neighbour : adjacencyMatrix.get(next)) {
                if (!path.contains(neighbour)) {
                    path.add(neighbour);
                    queue.add(neighbour);
                }
            }
            if (queue.isEmpty())
                return;
        }
    }

    private String rndSample(List<String> values, Set<String> exclude) {
        String jobID;
        do {
            jobID = values.get(rnd.nextInt(values.size()));
        } while (exclude.contains(jobID));
        return jobID;
    }

    @Override
    public void visit1(Op1 op) {
        //logger.info("OP1: {}", op);
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
        plan.setVars(vars);
        plan.pushJob(new ProjectJob(jobID, getPrevJobID(op.getSubOp()), vars));
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
    protected void visitFilter(OpFilter op) {
        this.visit1(op);
    }

    @Override
    protected void visitLeftJoin(OpLeftJoin op) {
        this.visit2(op);
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
        Triple t;
        for (Quad quad : op.getQuads()) {
            t = quad.asTriple();
            bgp.add(t);
            deleteTemplate.add(t);
        }
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
