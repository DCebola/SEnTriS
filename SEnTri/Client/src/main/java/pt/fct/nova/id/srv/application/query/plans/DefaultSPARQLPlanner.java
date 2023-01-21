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
import org.apache.jena.util.PrintUtil;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.fct.nova.id.srv.application.ontologies.Ontology;
import pt.fct.nova.id.srv.application.query.QueryType;
import pt.fct.nova.id.srv.application.query.jobs.EmptyResJob;
import pt.fct.nova.id.srv.application.query.jobs.SearchJob;
import pt.fct.nova.id.srv.application.query.jobs.SerializableSortCondition;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.DistinctJob;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.OrderByJob;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.ProjectJob;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.SliceJob;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.JoinJob;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.MinusJob;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.OptionalJob;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.UnionJob;

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
    }

    private void generateGetJobs(OpBGP op) {
        List<Triple> patterns = op.getPattern().getList();
        int total_patterns = patterns.size();
        List<SearchJob> searchJobs = new ArrayList<>(total_patterns);
        Node s, p, o;
        for (Triple t : patterns) {
            s = t.getSubject();
            p = t.getPredicate();
            o = t.getObject();
            if (ontology != null) {
                if (p.equals(RDF.type.asNode()) && !o.isVariable())
                    expandClass("PATTERN", s, o, 0);
                else if (!p.isVariable())
                    expandProperty("PATTERN", s, p, o, 0);
            }

            String jobID = generateID();
            if (total_patterns == 1) {
                parsed_op.put(op, jobID);
                plan.pushJob(new SearchJob(jobID, extractVariablesPattern(s, p, o), s, p, o));
            } else
                searchJobs.add(new SearchJob(jobID, extractVariablesPattern(s, p, o), s, p, o));
        }
        if (total_patterns >= 2)
            generateRandomJoinPipeline(op, searchJobs);
    }

    private void expandProperty(String prefix, Node s, Node p, Node o, int depth) {
        assert ontology != null;
        if (depth == ontology.getMaximumExpansionDepth())
            return;
        System.out.println("[" + prefix + "," + depth + "] - " + Triple.create(s, p, o));
        if (ontology.isSymmetric(p)) {
            //System.out.println("[SYMMETRIC," + depth + "] - " + Triple.create(o, p, s));
            expandProperty("SYMMETRIC", o, p, s, depth + 1);
        }
        if (ontology.isTransitive(p) && !o.isVariable()) {
            System.out.println("[TRANSITIVE," + depth + "] - " + PrintUtil.print(Triple.create(s, p, Var.alloc(p.getLocalName().concat(Integer.toString(0))))));
            for (int i = 1; i < ontology.getTransitivityDepth(); i++) {
                System.out.println("[TRANSITIVE," + depth + "] - " + PrintUtil.print(Triple.create(Var.alloc(p.getLocalName().concat(Integer.toString(i - 1))), p, Var.alloc(p.getLocalName().concat(Integer.toString(i))))));
            }
            System.out.println("[TRANSITIVE," + depth + "] - " + PrintUtil.print(Triple.create(
                    Var.alloc(p.getLocalName().concat(Integer.toString(ontology.getTransitivityDepth()))), p, o)));
        }
        boolean shouldExpand = true;
        for (OntProperty subProperty : ontology.getSubProperties(p)) {
            //System.out.println("[SUB-PROPERTY," + depth + "]" + PrintUtil.print(Triple.create(s, subProperty.asNode(), o)));
            if (depth != 0) {
                for (OntProperty superProperty : subProperty.listSuperProperties().toSet()) {
                    if (superProperty.asNode().equals(p)) {
                        shouldExpand = false;
                        break;
                    }
                }
            }
            if (shouldExpand)
                expandProperty("SUB-PROPERTY", s, subProperty.asNode(), o, depth + 1);
        }

        for (OntProperty equivalentProperty : ontology.getEquivalentProperties(p)) {
            //System.out.println("[EQUIVALENT," + depth + "]" + PrintUtil.print(Triple.create(s, equivalentProperty.asNode(), o)));
            expandProperty("EQUIVALENT", s, equivalentProperty.asNode(), o, depth + 1);
        }

        for (OntProperty inverseOf : ontology.getInverseOf(p)) {
            //System.out.println("[INVERSE," + depth + "]" + PrintUtil.print(Triple.create(s, inverseOf.asNode(), o)));
            expandProperty("INVERSE", s, inverseOf.asNode(), o, depth + 1);
        }
    }

    private void expandClass(String prefix, Node s, Node o, int depth) {
        assert ontology != null;
        if (depth == ontology.getMaximumExpansionDepth())
            return;
        Node rdfType = RDF.type.asNode();
        System.out.println("[" + prefix + ", " + depth + "]-" + PrintUtil.print(Triple.create(s, rdfType, o)));
        Set<OntClass> intersection = ontology.getIntersection(o);
        if (!intersection.isEmpty()) {
            for (OntClass c : intersection)
                expandClass("INTERSECTION", s, c.asNode(), depth + 1);
        } else {
            Restriction restriction = ontology.getRestriction(o);
            Var v;
            Node property;
            Node value;
            if (restriction != null && (restriction.isHasValueRestriction() || restriction.isSomeValuesFromRestriction())) {
                if (restriction.isHasValueRestriction()) {
                    v = Var.alloc(restriction.getOnProperty().asNode().getLocalName());
                    property = restriction.getOnProperty().asNode();
                    value = restriction.asHasValueRestriction().getHasValue().asNode();
                } else {
                    v = Var.alloc(restriction.asSomeValuesFromRestriction().getSomeValuesFrom().asNode().getLocalName());
                    property = restriction.getOnProperty().asNode();
                    value = restriction.asSomeValuesFromRestriction().getSomeValuesFrom().asNode();
                }
                //System.out.println("[RESTRICTION, " + depth + "] - " + PrintUtil.print(Triple.create(s, property, v)));
                expandProperty("RESTRICTION", s, property, v, depth + 1);
                expandClass("RESTRICTION", v, value, depth + 1);
            }
        }

        for (OntClass subClass : ontology.getSubClasses(o)) {
            //System.out.println("[SUBCLASS, " + depth + "] - " + PrintUtil.print(Triple.create(s, rdfType, subClass.asNode())));
            expandClass("SUBCLASS", s, subClass.asNode(), depth + 1);

        }
        for (OntClass equivalentClass : ontology.getEquivalentClasses(o)) {
            //System.out.println("[EQUIVALENT, " + depth + "] - " + PrintUtil.print(Triple.create(s, rdfType, equivalentClass.asNode())));
            expandClass("EQUIVALENT", s, equivalentClass.asNode(), depth + 1);
        }
    }


    private void generateRandomJoinPipeline(OpBGP op, List<SearchJob> searchJobs) {
        int numJobs = searchJobs.size();
        Map<Integer, List<Integer>> graph = new HashMap<>(numJobs);
        List<Integer> jobs = new ArrayList<>(numJobs);
        Set<Var> allVars = new HashSet<>();
        generateAdjacencyMatrix(searchJobs, numJobs, graph, jobs, allVars);

        List<Integer> path = new ArrayList<>(numJobs);
        Set<Integer> sampled = new HashSet<>();
        int next;
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
            SearchJob job1, job2;
            List<JoinJob> joins = new LinkedList<>();
            int j1, j2;
            String joinID;
            for (int i = 0; i < numJobs - 1; i += 2) {
                j1 = path.get(i);
                j2 = path.get(i + 1);
                job1 = searchJobs.get(j1);
                job2 = searchJobs.get(j2);
                plan.pushJob(job1);
                plan.pushJob(job2);
                joinID = generateID();
                joins.add(new JoinJob(joinID, job1.getID(), job2.getID()));
                System.out.println("[" + joinID + "] - (" + j1 + "," + j2 + ")");
            }
            if (numJobs % 2 == 1) {
                job1 = searchJobs.get(path.get(searchJobs.size() - 1));
                JoinJob join = joins.remove(joins.size() - 1);
                plan.pushJob(job1);
                plan.pushJob(join);
                joinID = generateID();
                joins.add(new JoinJob(joinID, job1.getID(), join.getID()));
                System.out.println("[" + joinID + "] - (" + path.get(searchJobs.size() - 1) + "," + join.getID() + ")");
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


    private static void generateAdjacencyMatrix(List<SearchJob> searchJobs, int numJobs, Map<Integer, List<Integer>> graph, List<Integer> jobs, Set<Var> allVars) {
        SearchJob job1, job2;
        List<Integer> edges;
        Set<Var> v2, v1;
        for (int i = 0; i < numJobs; i++) {
            job1 = searchJobs.get(i);
            jobs.add(i);
            v1 = extractVars(job1);
            allVars.addAll(v1);
            System.out.println("[" + i + "] - " + Arrays.toString(v1.toArray()));
            edges = new ArrayList<>(numJobs);
            for (int j = 0; j < numJobs; j++) {
                job2 = searchJobs.get(j);
                v2 = extractVars(job2);
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
