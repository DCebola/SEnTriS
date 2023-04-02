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
import pt.fct.nova.id.srv.application.query.jobs.jobs1.*;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.*;
import pt.fct.nova.id.srv.presentation.controllers.ParsingUtils;

import java.util.*;

import static pt.fct.nova.id.srv.application.query.QueryUtils.*;
import static pt.fct.nova.id.srv.application.query.QueryUtils.generateID;
import static pt.fct.nova.id.srv.application.query.jobs.VariablesPattern.*;
import static pt.fct.nova.id.srv.application.query.plans.InferenceClassExpansionOpType.*;
import static pt.fct.nova.id.srv.application.query.plans.InferencePropertyExpansionOpType.*;

public class SecureSPARQLPlanner extends OpVisitorByType implements SPARQLPlanner {
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
    private final Ontology ontology;

    public SecureSPARQLPlanner() {
        this.keywords = new HashSet<>();
        this.parsed_op = new HashMap<>();
        this.obfuscationMap = new HashMap<>();
        this.plan = new DefaultQueryExecutionPlan();
        this.searchJobsIDs = new HashSet<>();
        this.rnd = new Random();
        this.uploadTemplate = new LinkedList<>();
        this.deleteTemplate = new LinkedList<>();
        this.ontology = null;
    }

    public SecureSPARQLPlanner(Ontology ontology) {
        this.keywords = new HashSet<>();
        this.parsed_op = new HashMap<>();
        this.obfuscationMap = new HashMap<>();
        this.plan = new DefaultQueryExecutionPlan();
        this.searchJobsIDs = new HashSet<>();
        this.rnd = new Random();
        this.uploadTemplate = new LinkedList<>();
        this.deleteTemplate = new LinkedList<>();
        this.ontology = ontology;
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
            if (job instanceof SecureSearchJob searchJob)
                bgp.put(jobID, new HashSet<>(Arrays.asList(searchJob.getVars())));
            else if (job instanceof Job2 job2)
                bgp.put(jobID, extractVarsOfJob2(jobs, job2));
        }

        if (patterns.size() > 1) {
            generateRandomJoinPipeline(op, bgp);
        } else if (jobID != null)
            parsed_op.put(op, jobID);
    }

    private SecureSearchJob generateSecureSearchJob(Var var, Node node2, Node node3, VariablesPattern pattern) throws InvalidNodeException {
        Map<Var, String> searches = new HashMap<>();
        var = obfuscateVar(var);
        String jobID = generateID();
        String keyword = ParsingUtils.generateKeyword(pattern, ParsingUtils.parseKeyword(node2), ParsingUtils.parseKeyword(node3));
        searches.put(var, keyword);
        keywords.add(keyword);
        searchJobsIDs.add(jobID);
        return new SecureSearchJob(jobID, new Var[]{var}, searches);
    }

    private SecureSearchJob generateSecureSearchJob(Var var1, Var var2, Node node, VariablesPattern pattern) throws InvalidNodeException {
        Map<Var, String> searches = new HashMap<>();
        String jobID = generateID();
        String keyword = ParsingUtils.generateKeyword(pattern, ParsingUtils.parseKeyword(node));
        keywords.add(keyword);
        searchJobsIDs.add(jobID);
        var1 = obfuscateVar(var1);
        var2 = obfuscateVar(var2);
        searches.put(var1, keyword);
        searches.put(var2, keyword);
        return new SecureSearchJob(jobID, new Var[]{var1, var2}, searches);
    }

    private Set<Var> extractVarsOfJob2(Map<String, Job> jobs, Job2 job) {
        Set<Var> vars = new HashSet<>();
        Queue<Job> toBeProcessed = new LinkedList<>();
        toBeProcessed.add(jobs.get(job.getLeftJobID()));
        toBeProcessed.add(jobs.get(job.getRightJobID()));
        Job next;
        while (!toBeProcessed.isEmpty()) {
            next = toBeProcessed.poll();
            if (next instanceof SecureSearchJob searchJob)
                vars.addAll(Arrays.asList(searchJob.getVars()));
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
            SecureSearchJob job = switch (extractVariablesPattern(s, p, o)) {
                case S -> generateSecureSearchJob(Var.alloc(s), p, o, S);
                case P -> generateSecureSearchJob(Var.alloc(p), s, o, P);
                case O -> generateSecureSearchJob(Var.alloc(o), s, p, O);
                case SP -> generateSecureSearchJob(Var.alloc(s), Var.alloc(p), o, SP);
                case SO -> generateSecureSearchJob(Var.alloc(s), Var.alloc(o), p, SO);
                case PO -> generateSecureSearchJob(Var.alloc(p), Var.alloc(o), s, PO);
                case SPO -> null;
            };
            assert job != null;
            jobID = job.getID();
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
        List<InferenceClassExpansionOpType> ops = new ArrayList<>(List.of(SUBCLASS, EQUIVALENT_CLASS, INTERSECTION_OPERAND, RESTRICTION, INTERSECTION));
        Collections.shuffle(ops);
        for (InferenceClassExpansionOpType op : ops) {
            switch (op) {
                case SUBCLASS ->
                        jobID = expandClassDisjunction("SUBCLASS", ontology.getSubClasses(o), jobID, s, depth, jobs, jobsIDs, rdfType);
                case EQUIVALENT_CLASS ->
                        jobID = expandClassDisjunction("EQUIVALENT", ontology.getEquivalentClasses(o), jobID, s, depth, jobs, jobsIDs, rdfType);
                case INTERSECTION_OPERAND ->
                        jobID = expandClassDisjunction("INTERSECTION-OPERAND", ontology.getIntersectionWhereClassIsOperand(o), jobID, s, depth, jobs, jobsIDs, rdfType);
                case RESTRICTION ->
                        jobID = expandRestriction("RESTRICTION", ontology.getRestriction(o), jobID, s, depth, jobs, jobsIDs, rdfType);
                case INTERSECTION -> {
                    Collection<? extends OntClass> intersection = ontology.getIntersection(o);
                    if (!intersection.isEmpty())
                        jobID = expandClassConjunction("INTERSECTION", intersection, jobID, s, depth, jobs, jobsIDs, rdfType);
                }
            }
        }
        return jobID;
    }

    private String expandRestriction(String prefix, Restriction restriction, String jobID, Node s, int depth,
                                     Map<String, Job> jobs, Map<String, String> jobsIDs, Node rdfType) throws InvalidNodeException {
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
        List<InferencePropertyExpansionOpType> ops = new ArrayList<>(List.of(SUBPROPERTY, EQUIVALENT_PROPERTY, SYMMETRIC, TRANSITIVE, INVERSE_OF));
        Collections.shuffle(ops);
        for (InferencePropertyExpansionOpType op : ops) {
            switch (op) {
                case SUBPROPERTY ->
                        jobID = expandProperties("SUB-PROPERTY", ontology.getSubProperties(p), jobID, s, o, depth, jobs, jobsIDs, canExpandSymmetric, canExpandInverseOf);
                case EQUIVALENT_PROPERTY ->
                        jobID = expandProperties("EQUIVALENT-PROPERTY", ontology.getEquivalentProperties(p), jobID, s, o, depth, jobs, jobsIDs, canExpandSymmetric, canExpandInverseOf);
                case SYMMETRIC -> {
                    if (canExpandSymmetric && ontology.isSymmetric(p)) {
                        jobID = pushUnion(jobID, pushSearch(o, p, s, jobs, jobsIDs), jobs, jobsIDs);
                        jobID = expandProperty("SYMMETRIC", jobID, o, p, s, depth + 1, jobs, jobsIDs, false, canExpandInverseOf);
                    }
                }
                case TRANSITIVE -> {
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
                }
                case INVERSE_OF -> {
                    if (canExpandInverseOf)
                        jobID = expandProperties("INVERSE", ontology.getInverseOf(p), jobID, o, s, depth, jobs, jobsIDs, canExpandSymmetric, false);
                }
            }
        }
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
        op.getQuads().forEach(quad -> deleteTemplate.add(quad.asTriple()));
        OpWalker.walk(new OpBGP(BasicPattern.wrap(deleteTemplate)), this);
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
