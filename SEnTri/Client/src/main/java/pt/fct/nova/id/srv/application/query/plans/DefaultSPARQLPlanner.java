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
import pt.fct.nova.id.srv.application.ontologies.Ontology;
import pt.fct.nova.id.srv.application.query.QueryType;
import pt.fct.nova.id.srv.application.query.jobs.*;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.DistinctJob;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.OrderByJob;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.ProjectJob;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.SliceJob;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.*;
import pt.fct.nova.id.srv.application.schemes.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.presentation.controllers.ParsingUtils;

import java.util.*;

import static pt.fct.nova.id.srv.application.query.QueryUtils.*;

public class DefaultSPARQLPlanner extends OpVisitorByType implements SPARQLPlanner {
    private final Map<Op, String> parsed_op;
    private final DefaultQueryExecutionPlan plan;
    private final Random rnd;
    private final Set<Triple> uploadTemplate;
    private final Set<Triple> deleteTemplate;
    private QueryType queryType;
    private List<Triple> constructTemplate;
    private final Ontology ontology;
    private final Set<Var> vars;

    public DefaultSPARQLPlanner() {
        this.parsed_op = new HashMap<>();
        this.plan = new DefaultQueryExecutionPlan();
        this.rnd = new Random();
        this.constructTemplate = new LinkedList<>();
        this.uploadTemplate = new HashSet<>();
        this.deleteTemplate = new HashSet<>();
        this.ontology = null;
        this.vars = new HashSet<>();
    }

    public DefaultSPARQLPlanner(Ontology ontology) {
        this.parsed_op = new HashMap<>();
        this.plan = new DefaultQueryExecutionPlan();
        this.rnd = new Random();
        this.constructTemplate = new LinkedList<>();
        this.uploadTemplate = new HashSet<>();
        this.deleteTemplate = new HashSet<>();
        this.ontology = ontology;
        this.vars = new HashSet<>();
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
        if (plan.getVars().isEmpty())
            plan.setVars(vars.stream().toList());
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
        if (plan.getVars().isEmpty())
            plan.setVars(vars.stream().toList());
        return plan;
    }

    @Override
    public void visit0(Op0 op) {
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
            throw new RuntimeException(e.getMessage());
        }
    }

    private void generateGetJobs(OpBGP op) throws InvalidNodeException {
        List<Triple> patterns = op.getPattern().getList();
        Map<String, String> jobIDs = new HashMap<>();
        Map<String, Job> jobs = new HashMap<>();
        Map<String, Set<Var>> bgp = new HashMap<>();
        Node s, p, o;
        String jobID = null;
        Job job;
        for (Triple t : patterns) {
            s = t.getSubject();
            p = t.getPredicate();
            o = t.getObject();
            jobID = pushSearch(s, p, o, jobs, jobIDs);
            if (ontology != null) {
                if (p.equals(RDF.type.asNode()) && !o.isVariable())
                    jobID = expandClass("PATTERN", jobID, s, o, 0, jobs, jobIDs);
                else if (!p.isVariable())
                    jobID = expandProperty("PATTERN", jobID, s, p, o, 0, jobs, jobIDs, true, true);
            }
            job = jobs.get(jobID);
            Set<Var> extractedVars;
            if (job instanceof SearchJob searchJob) {
                extractedVars = extractVars(searchJob);
                vars.addAll(extractedVars);
                bgp.put(jobID, extractVars(searchJob));
            } else if (job instanceof Job2 job2)
                bgp.put(jobID, extractVarsOfJob2(jobs, job2));

        }
        if (patterns.size() > 1) {
            generateRandomJoinPipeline(op, bgp);
        } else if (jobID != null)
            parsed_op.put(op, jobID);
    }

    private Set<Var> extractVarsOfJob2(Map<String, Job> jobs, Job2 job) {
        Set<Var> vars = new HashSet<>();
        Queue<Job> toBeProcessed = new LinkedList<>();
        toBeProcessed.add(jobs.get(job.getLeftJobID()));
        toBeProcessed.add(jobs.get(job.getRightJobID()));
        Job next;
        while (!toBeProcessed.isEmpty()) {
            next = toBeProcessed.poll();
            if (next instanceof SearchJob searchJob)
                vars.addAll(extractVars(searchJob));
            else if (next instanceof Job2 job2) {
                toBeProcessed.add(jobs.get(job2.getLeftJobID()));
                toBeProcessed.add(jobs.get(job2.getRightJobID()));
            }
        }
        return vars;
    }


    private String pushSearch(Node s, Node p, Node o, Map<String, Job> jobs, Map<String, String> jobIDs) throws InvalidNodeException {
        String jobSignature = ParsingUtils.parseTriple(s, p, o);
        String jobID = jobIDs.get(jobSignature);
        if (jobID == null) {
            jobID = generateID();
            SearchJob job = new SearchJob(jobID, extractVariablesPattern(s, p, o), s, p, o);
            jobs.put(jobID, job);
            jobIDs.put(jobSignature, jobID);
            plan.pushJob(job);
        }
        System.out.println("[SEARCH, " + jobID + "] - " + s + " | " + p + " | " + o);
        return jobID;
    }

    private String pushUnion(String left, String right, Map<String, Job> jobs, Map<String, String> jobIDs) {
        String jobSignature = left.concat(right);
        String jobID = jobIDs.get(jobSignature);
        if (jobID == null) {
            MinusJob minusJob = new MinusJob(generateID(), right, left);
            jobID = generateID();
            UnionJob job = new UnionJob(jobID, left, minusJob.getID());
            jobs.put(jobID, job);
            jobIDs.put(jobSignature, jobID);
            plan.pushJob(minusJob);
            plan.pushJob(job);
        }
        System.out.println("[UNION, " + jobID + "] - " + left + " | " + right);
        return jobID;
    }

    private String pushJoin(String left, String right, Map<String, Job> jobs, Map<String, String> jobIDs) {
        String jobSignature = left.concat(right);
        String jobID = jobIDs.get(jobSignature);
        if (jobID == null) {
            jobID = generateID();
            JoinJob job = new JoinJob(jobID, left, right);
            jobs.put(jobID, job);
            jobIDs.put(jobSignature, jobID);
            plan.pushJob(job);
        }
        System.out.println("[JOIN, " + jobID + "] - " + left + " | " + right);
        return jobID;
    }

    private String expandClass(String prefix, String jobID, Node s, Node o, int depth, Map<String, Job> jobs, Map<String, String> jobsIDs) throws InvalidNodeException {
        assert ontology != null;
        if (depth == ontology.getMaximumExpansionDepth())
            return jobID;
        Node rdfType = RDF.type.asNode();
        System.out.println("[" + prefix + "," + depth + "] - " + Triple.create(s, rdfType, o));
        jobID = expandClassDisjunction("SUBCLASS", ontology.getSubClasses(o), jobID, s, depth, jobs, jobsIDs, rdfType);
        jobID = expandClassDisjunction("EQUIVALENT", ontology.getEquivalentClasses(o), jobID, s, depth, jobs, jobsIDs, rdfType);
        jobID = expandClassDisjunction("INTERSECTION-OPERAND", ontology.getIntersectionWhereClassIsOperand(o), jobID, s, depth, jobs, jobsIDs, rdfType);
        jobID = expandRestriction("RESTRICTION", ontology.getRestriction(o), jobID, s, depth, jobs, jobsIDs, rdfType);
        Collection<? extends OntClass> intersection = ontology.getIntersection(o);
        if (!intersection.isEmpty())
            jobID = expandClassConjunction("INTERSECTION", intersection, jobID, s, depth, jobs, jobsIDs, rdfType);
        return jobID;
    }

    private String expandRestriction(String prefix, Restriction restriction, String jobID, Node s, int depth, Map<String, Job> jobs,
                                     Map<String, String> jobsIDs, Node rdfType) throws InvalidNodeException {
        Var var;
        Node property;
        String right, left, join;
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
            right = pushSearch(s, property, var, jobs, jobsIDs);
            left = pushSearch(var, rdfType, value, jobs, jobsIDs);
            join = pushJoin(
                    expandProperty(prefix.concat(" PROPERTY"), right, s, property, var, depth, jobs, jobsIDs, true, true),
                    expandClass(prefix.concat(" VALUE CLASS"), left, var, value, depth, jobs, jobsIDs), jobs, jobsIDs);
            jobID = pushUnion(jobID, join, jobs, jobsIDs);
        }
        return jobID;
    }

    private String expandClassConjunction(String prefix, Collection<? extends OntClass> ontClasses, String jobID, Node s, int depth, Map<String, Job> jobs,
                                          Map<String, String> jobsIDs, Node rdfType) throws InvalidNodeException {
        String search, right = null, left;
        for (OntClass ontClass : ontClasses) {
            search = pushSearch(s, rdfType, ontClass.asNode(), jobs, jobsIDs);
            left = expandClass(prefix.concat(" CLASS"), search, s, ontClass.asNode(), depth + 1, jobs, jobsIDs);
            if (right != null)
                right = pushJoin(right, left, jobs, jobsIDs);
            else
                right = left;
        }
        if (right != null)
            return pushUnion(jobID, right, jobs, jobsIDs);
        return jobID;
    }

    private String expandClassDisjunction(String prefix, Collection<OntClass> ontClasses, String jobID, Node s, int depth, Map<String, Job> jobs,
                                          Map<String, String> jobsIDs, Node rdfType) throws InvalidNodeException {
        String search, right = null, left;
        for (OntClass ontClass : ontClasses) {
            search = pushSearch(s, rdfType, ontClass.asNode(), jobs, jobsIDs);
            left = expandClass(prefix.concat(" CLASS"), search, s, ontClass.asNode(), depth + 1, jobs, jobsIDs);
            if (right != null)
                right = pushUnion(right, left, jobs, jobsIDs);
            else
                right = left;
        }
        if (right != null)
            return pushUnion(jobID, right, jobs, jobsIDs);
        return jobID;
    }

    private String expandProperty(String prefix, String jobID, Node s, Node p, Node o, int depth,
                                  Map<String, Job> jobs, Map<String, String> jobsIDs, boolean canExpandSymmetric, boolean canExpandInverseOf) throws InvalidNodeException {
        assert ontology != null;
        if (depth == ontology.getMaximumExpansionDepth())
            return jobID;

        System.out.println("[" + prefix + "," + depth + "] - " + Triple.create(s, p, o));
        if (canExpandSymmetric && ontology.isSymmetric(p)) {
            jobID = pushUnion(jobID, pushSearch(o, p, s, jobs, jobsIDs), jobs, jobsIDs);
            jobID = expandProperty("SYMMETRIC", jobID, o, p, s, depth + 1, jobs, jobsIDs, false, canExpandInverseOf);
        }
        if (ontology.isTransitive(p)) {
            System.out.println("#####");
            Var var = Var.alloc(p.getLocalName().concat(Integer.toString(0))), nextVar;
            String leftID = pushSearch(s, p, var, jobs, jobsIDs);
            jobID = pushUnion(jobID, pushJoin(leftID, pushSearch(var, p, o, jobs, jobsIDs), jobs, jobsIDs), jobs, jobsIDs);
            for (int i = 1; i < ontology.getTransitivityDepth(); i++) {
                for (int j = 1; j < i + 1; j++) {
                    if (j == i) {
                        nextVar = Var.alloc(p.getLocalName().concat(Integer.toString(j)));
                        leftID = pushJoin(leftID, pushSearch(var, p, nextVar, jobs, jobsIDs), jobs, jobsIDs);
                        var = nextVar;
                    }
                }
                jobID = pushUnion(jobID, pushJoin(leftID, pushSearch(var, p, o, jobs, jobsIDs), jobs, jobsIDs), jobs, jobsIDs);
            }
            System.out.println("#####");
        }

        jobID = expandProperties("SUB-PROPERTY", ontology.getSubProperties(p), jobID, s, o, depth, jobs, jobsIDs, canExpandSymmetric, canExpandInverseOf);
        jobID = expandProperties("EQUIVALENT-PROPERTY", ontology.getEquivalentProperties(p), jobID, s, o, depth, jobs, jobsIDs, canExpandSymmetric, canExpandInverseOf);
        if (canExpandInverseOf)
            jobID = expandProperties("INVERSE", ontology.getInverseOf(p), jobID, o, s, depth, jobs, jobsIDs, canExpandSymmetric, false);
        return jobID;
    }

    private String expandProperties(String prefix, Collection<? extends OntProperty> ontProperties, String jobID, Node s, Node o, int depth,
                                    Map<String, Job> jobs, Map<String, String> jobsIDs, boolean canExpandSymmetric, boolean canExpandInverseOf) throws InvalidNodeException {
        for (OntProperty ontProperty : ontProperties) {
            jobID = pushUnion(jobID, pushSearch(s, ontProperty.asNode(), o, jobs, jobsIDs), jobs, jobsIDs);
            jobID = expandProperty(prefix, jobID, s, ontProperty.asNode(), o, depth + 1, jobs, jobsIDs, canExpandSymmetric, canExpandInverseOf);
        }
        return jobID;
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
            LinkedList<JoinJob> joins = new LinkedList<>();
            String jobID1, jobID2;
            String joinID;
            for (int i = 0; i < path.size() - 1; i += 1) {
                jobID1 = path.get(i);
                jobID2 = path.get(i + 1);
                joinID = generateID();
                path.set(i + 1, joinID);
                joins.add(new JoinJob(joinID, jobID1, jobID2));
                System.out.println("[" + joinID + "] - (" + jobID1 + "," + jobID2 + ")");
            }
            joins.forEach(plan::pushJob);
            parsed_op.put(op, joins.get(joins.size() - 1).getID());
        } else {
            String jobID = generateID();
            plan.pushJob(new EmptyResJob(jobID, allVars));
            parsed_op.put(op, jobID);
        }
    }


    private static void generateAdjacencyMatrix(Map<String, Set<Var>> bgp, int numJobs, Map<
            String, List<String>> graph, List<String> jobs, Set<Var> allVars) {
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

    private void bfsSearch(String root, Map<String, List<String>> adjacencyMatrix, List<String> path,
                           int depth) {
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
        System.out.println(op.toString());
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
