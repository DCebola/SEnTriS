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
import pt.fct.nova.id.srv.application.query.jobs.*;
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
import static pt.fct.nova.id.srv.application.query.jobs.VariablesPattern.SO;

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
        List<SearchJob> searchJobs = new LinkedList<>();
        List<UnionJob> unionJobs = new LinkedList<>();
        List<JoinJob> joinJobs = new LinkedList<>();
        Map<String, Set<Var>> bgp = new HashMap<>();
        Map<String, Set<Var>> jobsVars = new HashMap<>();
        Node s, p, o;
        Job job = null;
        for (Triple t : patterns) {
            s = t.getSubject();
            p = t.getPredicate();
            o = t.getObject();
            job = new SearchJob(generateID(), extractVariablesPattern(s, p, o), s, p, o);
            searchJobs.add((SearchJob) job);
            jobsVars.put(job.getID(), extractVars((SearchJob) job));
            System.out.println("[SEARCH, " + job.getID() + "] - " + s + " | " + p + " | " + o);
            if (ontology != null) {
                if (p.equals(RDF.type.asNode()) && !o.isVariable())
                    job = expandClass("PATTERN", job, s, o, 0, searchJobs, unionJobs, joinJobs, jobsVars);
                else if (!p.isVariable())
                    job = expandProperty(true, "PATTERN", job, s, p, o, 0, searchJobs, unionJobs, joinJobs, jobsVars);
                searchJobs.forEach(plan::pushJob);
                joinJobs.forEach(plan::pushJob);
                unionJobs.forEach(plan::pushJob);
                bgp.put(job.getID(), jobsVars.get(job.getID()));
            } else {
                plan.pushJob(job);
                bgp.put(job.getID(), extractVars((SearchJob) job));
            }
        }
        jobsVars.forEach((k, v) -> System.out.println("(" + k + "," + Arrays.toString(v.toArray()) + ")"));
        if (patterns.size() > 1) {
            generateRandomJoinPipeline(op, bgp);
        } else if (job != null)
            parsed_op.put(op, job.getID());
    }

    private Job expandClass(String prefix, Job previousJob, Node s, Node o, int depth, List<SearchJob> searchJobs,
                            List<UnionJob> unionJobs, List<JoinJob> joinJobs, Map<String, Set<Var>> jobsVars) {
        assert ontology != null;
        if (depth == ontology.getMaximumExpansionDepth())
            return previousJob;
        SearchJob searchJob1;
        UnionJob unionJob;
        Node rdfType = RDF.type.asNode();
        Set<OntClass> intersection = ontology.getIntersection(o);
        Set<Var> vars;
        System.out.println("[" + prefix + "," + depth + "] - " + Triple.create(s, rdfType, o));
        if (!intersection.isEmpty()) {
            for (OntClass ontClass : intersection) {
                if (!ontClass.isRestriction()) {
                    searchJob1 = new SearchJob(generateID(), extractVariablesPattern(s, rdfType, ontClass.asNode()), s, rdfType, ontClass.asNode());
                    System.out.println("[SEARCH, " + searchJob1.getID() + "] - " + searchJob1.getSubject() + " | " + searchJob1.getPredicate() + " | " + searchJob1.getObject());
                    unionJob = new UnionJob(generateID(), previousJob.getID(), searchJob1.getID());
                    System.out.println("[UNION, " + unionJob.getID() + "] - " + unionJob.getRightJobID() + " | " + unionJob.getLeftJobID());
                    jobsVars.put(searchJob1.getID(), extractVars(searchJob1));
                    vars = new HashSet<>(jobsVars.get(previousJob.getID()));
                    vars.addAll(jobsVars.get(searchJob1.getID()));
                    jobsVars.put(unionJob.getID(), vars);
                    searchJobs.add(searchJob1);
                    unionJobs.add(unionJob);
                    previousJob = unionJob;
                }
                previousJob = expandClass("INTERSECTION", previousJob, s, ontClass.asNode(), depth + 1, searchJobs, unionJobs, joinJobs, jobsVars);
            }
        } else {
            Restriction restriction = ontology.getRestriction(o);
            Var var;
            Node property;
            Node value;
            SearchJob searchJob2;
            JoinJob joinJob;
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
                searchJob1 = new SearchJob(generateID(), extractVariablesPattern(s, property, var), s, property, var);
                System.out.println("[SEARCH, " + searchJob1.getID() + "] - " + searchJob1.getSubject() + " | " + searchJob1.getPredicate() + " | " + searchJob1.getObject());

                searchJob2 = new SearchJob(generateID(), extractVariablesPattern(var, rdfType, value), var, rdfType, value);
                System.out.println("[SEARCH, " + searchJob2.getID() + "] - " + searchJob2.getSubject() + " | " + searchJob2.getPredicate() + " | " + searchJob2.getObject());

                joinJob = new JoinJob(generateID(), searchJob1.getID(), searchJob2.getID());
                System.out.println("[JOIN, " + joinJob.getID() + "] - " + joinJob.getRightJobID() + " | " + joinJob.getLeftJobID());
                unionJob = new UnionJob(generateID(), previousJob.getID(), joinJob.getID());
                System.out.println("[UNION, " + unionJob.getID() + "] - " + unionJob.getRightJobID() + " | " + unionJob.getLeftJobID());

                jobsVars.put(searchJob1.getID(), extractVars(searchJob1));
                jobsVars.put(searchJob2.getID(), extractVars(searchJob2));

                vars = new HashSet<>(jobsVars.get(searchJob1.getID()));
                vars.addAll(jobsVars.get(searchJob2.getID()));
                jobsVars.put(joinJob.getID(), vars);

                vars = new HashSet<>(jobsVars.get(previousJob.getID()));
                vars.addAll(jobsVars.get(joinJob.getID()));
                jobsVars.put(unionJob.getID(), vars);

                searchJobs.add(searchJob1);
                searchJobs.add(searchJob2);
                joinJobs.add(joinJob);
                unionJobs.add(unionJob);
                previousJob = unionJob;
                previousJob = expandProperty(true, "RESTRICTION", previousJob, s, property, var, depth + 1, searchJobs, unionJobs, joinJobs, jobsVars);
                previousJob = expandClass("RESTRICTION", previousJob, var, value, depth + 1, searchJobs, unionJobs, joinJobs, jobsVars);
            }
        }

        for (OntClass subClass : ontology.getSubClasses(o)) {
            if (!subClass.isRestriction()) {
                searchJob1 = new SearchJob(generateID(), extractVariablesPattern(s, rdfType, subClass.asNode()), s, rdfType, subClass.asNode());
                System.out.println("[SEARCH, " + searchJob1.getID() + "] - " + searchJob1.getSubject() + " | " + searchJob1.getPredicate() + " | " + searchJob1.getObject());

                unionJob = new UnionJob(generateID(), previousJob.getID(), searchJob1.getID());
                System.out.println("[UNION, " + unionJob.getID() + "] - " + unionJob.getRightJobID() + " | " + unionJob.getLeftJobID());
                jobsVars.put(searchJob1.getID(), extractVars(searchJob1));
                vars = new HashSet<>(jobsVars.get(previousJob.getID()));
                vars.addAll(jobsVars.get(searchJob1.getID()));
                jobsVars.put(unionJob.getID(), vars);
                searchJobs.add(searchJob1);
                unionJobs.add(unionJob);
                previousJob = unionJob;
            }
            previousJob = expandClass("SUBCLASS", previousJob, s, subClass.asNode(), depth + 1, searchJobs, unionJobs, joinJobs, jobsVars);
        }
        for (OntClass equivalentClass : ontology.getEquivalentClasses(o)) {
            if (!equivalentClass.isRestriction()) {
                searchJob1 = new SearchJob(generateID(), extractVariablesPattern(s, rdfType, equivalentClass.asNode()), s, rdfType, equivalentClass.asNode());
                System.out.println("[SEARCH, " + searchJob1.getID() + "] - " + searchJob1.getSubject() + " | " + searchJob1.getPredicate() + " | " + searchJob1.getObject());

                unionJob = new UnionJob(generateID(), previousJob.getID(), searchJob1.getID());
                System.out.println("[UNION, " + unionJob.getID() + "] - " + unionJob.getRightJobID() + " | " + unionJob.getLeftJobID());

                jobsVars.put(searchJob1.getID(), extractVars(searchJob1));
                vars = new HashSet<>(jobsVars.get(previousJob.getID()));
                vars.addAll(jobsVars.get(searchJob1.getID()));
                jobsVars.put(unionJob.getID(), vars);
                searchJobs.add(searchJob1);
                unionJobs.add(unionJob);
                previousJob = unionJob;
            }
            previousJob = expandClass("EQUIVALENT", previousJob, s, equivalentClass.asNode(), depth + 1, searchJobs, unionJobs, joinJobs, jobsVars);
        }

        for (OntClass intersectionWhereClassIsOperand : ontology.getIntersectionWhereClassIsOperand(o)) {
            if (!intersectionWhereClassIsOperand.isRestriction()) {
                searchJob1 = new SearchJob(generateID(), extractVariablesPattern(s, rdfType, intersectionWhereClassIsOperand.asNode()), s, rdfType, intersectionWhereClassIsOperand.asNode());
                System.out.println("[SEARCH, " + searchJob1.getID() + "] - " + searchJob1.getSubject() + " | " + searchJob1.getPredicate() + " | " + searchJob1.getObject());

                unionJob = new UnionJob(generateID(), previousJob.getID(), searchJob1.getID());
                System.out.println("[UNION, " + unionJob.getID() + "] - " + unionJob.getRightJobID() + " | " + unionJob.getLeftJobID());

                jobsVars.put(searchJob1.getID(), extractVars(searchJob1));
                vars = new HashSet<>(jobsVars.get(previousJob.getID()));
                vars.addAll(jobsVars.get(searchJob1.getID()));
                jobsVars.put(unionJob.getID(), vars);
                searchJobs.add(searchJob1);
                unionJobs.add(unionJob);
                previousJob = unionJob;
            }
            previousJob = expandClass("OPERAND-IN-INTERSECTION", previousJob, s, intersectionWhereClassIsOperand.asNode(), depth + 1, searchJobs, unionJobs, joinJobs, jobsVars);
        }
        return previousJob;
    }

    private Job expandProperty(boolean canExpandInverseOf, String prefix, Job previousJob, Node s, Node p, Node o, int depth,
                               List<SearchJob> searchJobs, List<UnionJob> unionJobs, List<JoinJob> joinJobs, Map<String, Set<Var>> jobsVars) {
        assert ontology != null;
        if (depth == ontology.getMaximumExpansionDepth())
            return previousJob;
        SearchJob searchJob;
        UnionJob unionJob;

        System.out.println("[" + prefix + "," + depth + "] - " + Triple.create(s, p, o));
        Set<Var> vars;
        if (ontology.isSymmetric(p)) {
            searchJob = new SearchJob(generateID(), extractVariablesPattern(o, p, s), o, p, s);
            unionJob = new UnionJob(generateID(), previousJob.getID(), searchJob.getID());
            jobsVars.put(searchJob.getID(), extractVars(searchJob));
            vars = new HashSet<>(jobsVars.get(previousJob.getID()));
            vars.addAll(jobsVars.get(searchJob.getID()));
            jobsVars.put(unionJob.getID(), vars);
            searchJobs.add(searchJob);
            unionJobs.add(unionJob);
            previousJob = unionJob;
            previousJob = expandProperty(true, "SYMMETRIC", previousJob, o, p, s, depth + 1, searchJobs, unionJobs, joinJobs, jobsVars);
        }
        if (ontology.isTransitive(p) && !o.isVariable()) {
            Var nextVar, previousVar, firstVar;
            JoinJob joinJob;
            String leftID;
            SearchJob firstSearch;
            firstVar = Var.alloc(p.getLocalName().concat(Integer.toString(0)));
            firstSearch = new SearchJob(generateID(), extractVariablesPattern(s, p, firstVar), s, p, firstVar);
            previousVar = firstVar;
            searchJob = new SearchJob(generateID(), extractVariablesPattern(previousVar, p, o), previousVar, p, o);
            joinJob = new JoinJob(generateID(), firstSearch.getID(), searchJob.getID());
            unionJob = new UnionJob(generateID(), previousJob.getID(), joinJob.getID());

            jobsVars.put(firstSearch.getID(), extractVars(firstSearch));
            jobsVars.put(searchJob.getID(), extractVars(searchJob));
            vars = new HashSet<>(jobsVars.get(firstSearch.getID()));
            vars.addAll(jobsVars.get(searchJob.getID()));
            jobsVars.put(joinJob.getID(), vars);
            vars = new HashSet<>(jobsVars.get(previousJob.getID()));
            vars.addAll(jobsVars.get(joinJob.getID()));
            jobsVars.put(unionJob.getID(), vars);

            searchJobs.add(firstSearch);
            searchJobs.add(searchJob);
            joinJobs.add(joinJob);
            unionJobs.add(unionJob);
            previousJob = unionJob;

            System.out.println("#####");
            System.out.println("[TRANSITIVE," + 0 + "] - " + PrintUtil.print(Triple.create(s, p, firstVar)));
            System.out.println("[TRANSITIVE," + 0 + "] - " + PrintUtil.print(Triple.create(previousVar, p, o)));
            System.out.println("[SEARCH, " + firstSearch.getID() + "] - " + firstSearch.getSubject() + " | " + firstSearch.getPredicate() + " | " + firstSearch.getObject());
            System.out.println("[SEARCH, " + searchJob.getID() + "] - " + searchJob.getSubject() + " | " + searchJob.getPredicate() + " | " + searchJob.getObject());
            System.out.println("[JOIN, " + joinJob.getID() + "] - " + joinJob.getRightJobID() + " | " + joinJob.getLeftJobID());
            System.out.println("[UNION, " + unionJob.getID() + "] - " + unionJob.getRightJobID() + " | " + unionJob.getLeftJobID());
            System.out.println("#####");

            searchJob = firstSearch;
            leftID = searchJob.getID();
            for (int i = 1; i < ontology.getTransitivityDepth(); i++) {
                System.out.println("[TRANSITIVE," + i + "] - " + PrintUtil.print(Triple.create(s, p, previousVar)));
                for (int j = 1; j < i + 1; j++) {
                    if (j == i) {
                        nextVar = Var.alloc(p.getLocalName().concat(Integer.toString(j)));
                        System.out.println("[TRANSITIVE," + i + "] - " + PrintUtil.print(Triple.create(previousVar, p, nextVar)));
                        searchJob = new SearchJob(generateID(), SO, previousVar, p, nextVar);
                        joinJob = new JoinJob(generateID(), leftID, searchJob.getID());
                        previousVar = nextVar;
                        jobsVars.put(searchJob.getID(), extractVars(searchJob));
                        vars = new HashSet<>(jobsVars.get(leftID));
                        vars.addAll(jobsVars.get(searchJob.getID()));
                        jobsVars.put(joinJob.getID(), vars);
                        leftID = joinJob.getID();
                        searchJobs.add(searchJob);
                        joinJobs.add(joinJob);
                        System.out.println("[SEARCH, " + searchJob.getID() + "] - " + searchJob.getSubject() + " | " + searchJob.getPredicate() + " | " + searchJob.getObject());
                        System.out.println("[JOIN, " + joinJob.getID() + "] - " + joinJob.getRightJobID() + " | " + joinJob.getLeftJobID());
                    } else
                        System.out.println(j);
                }
                searchJob = new SearchJob(generateID(), extractVariablesPattern(previousVar, p, o), previousVar, p, o);
                joinJob = new JoinJob(generateID(), leftID, searchJob.getID());
                unionJob = new UnionJob(generateID(), previousJob.getID(), joinJob.getID());

                jobsVars.put(searchJob.getID(), extractVars(searchJob));
                vars = new HashSet<>(jobsVars.get(leftID));
                vars.addAll(jobsVars.get(searchJob.getID()));
                jobsVars.put(joinJob.getID(), vars);
                vars = new HashSet<>(jobsVars.get(previousJob.getID()));
                vars.addAll(jobsVars.get(joinJob.getID()));
                jobsVars.put(unionJob.getID(), vars);

                searchJobs.add(searchJob);
                joinJobs.add(joinJob);
                unionJobs.add(unionJob);
                System.out.println("[TRANSITIVE," + i + "] - " + PrintUtil.print(Triple.create(previousVar, p, o)));
                System.out.println("[SEARCH, " + searchJob.getID() + "] - " + searchJob.getSubject() + " | " + searchJob.getPredicate() + " | " + searchJob.getObject());
                System.out.println("[JOIN, " + joinJob.getID() + "] - " + joinJob.getRightJobID() + " | " + joinJob.getLeftJobID());
                System.out.println("[UNION, " + unionJob.getID() + "] - " + unionJob.getRightJobID() + " | " + unionJob.getLeftJobID());
                System.out.println("#######");
                previousJob = unionJob;
            }

        }

        for (OntProperty subProperty : ontology.getSubProperties(p)) {
            searchJob = new SearchJob(generateID(), extractVariablesPattern(s, subProperty.asNode(), o), s, subProperty.asNode(), o);
            System.out.println("[SEARCH, " + searchJob.getID() + "] - " + searchJob.getSubject() + " | " + searchJob.getPredicate() + " | " + searchJob.getObject());
            unionJob = new UnionJob(generateID(), previousJob.getID(), searchJob.getID());
            System.out.println("[UNION, " + unionJob.getID() + "] - " + unionJob.getRightJobID() + " | " + unionJob.getLeftJobID());
            jobsVars.put(searchJob.getID(), extractVars(searchJob));
            vars = new HashSet<>(jobsVars.get(previousJob.getID()));
            vars.addAll(jobsVars.get(searchJob.getID()));
            jobsVars.put(unionJob.getID(), vars);
            searchJobs.add(searchJob);
            unionJobs.add(unionJob);
            previousJob = unionJob;
            previousJob = expandProperty(true, "SUB-PROPERTY", previousJob, s, subProperty.asNode(), o, depth + 1, searchJobs, unionJobs, joinJobs, jobsVars);

        }

        for (OntProperty equivalentProperty : ontology.getEquivalentProperties(p)) {
            searchJob = new SearchJob(generateID(), extractVariablesPattern(s, equivalentProperty.asNode(), o), s, equivalentProperty.asNode(), o);
            System.out.println("[SEARCH, " + searchJob.getID() + "] - " + searchJob.getSubject() + " | " + searchJob.getPredicate() + " | " + searchJob.getObject());
            unionJob = new UnionJob(generateID(), previousJob.getID(), searchJob.getID());
            System.out.println("[UNION, " + unionJob.getID() + "] - " + unionJob.getRightJobID() + " | " + unionJob.getLeftJobID());
            jobsVars.put(searchJob.getID(), extractVars(searchJob));
            vars = new HashSet<>(jobsVars.get(previousJob.getID()));
            vars.addAll(jobsVars.get(searchJob.getID()));
            jobsVars.put(unionJob.getID(), vars);
            searchJobs.add(searchJob);
            unionJobs.add(unionJob);
            previousJob = unionJob;
            previousJob = expandProperty(true, "EQUIVALENT", previousJob, s, equivalentProperty.asNode(), o, depth + 1, searchJobs, unionJobs, joinJobs, jobsVars);
        }
        if (canExpandInverseOf)
            for (OntProperty inverseOf : ontology.getInverseOf(p)) {
                searchJob = new SearchJob(generateID(), extractVariablesPattern(o, inverseOf.asNode(), s), o, inverseOf.asNode(), s);
                System.out.println("[SEARCH, " + searchJob.getID() + "] - " + searchJob.getSubject() + " | " + searchJob.getPredicate() + " | " + searchJob.getObject());
                unionJob = new UnionJob(generateID(), previousJob.getID(), searchJob.getID());
                System.out.println("[UNION, " + unionJob.getID() + "] - " + unionJob.getRightJobID() + " | " + unionJob.getLeftJobID());
                jobsVars.put(searchJob.getID(), extractVars(searchJob));
                vars = new HashSet<>(jobsVars.get(previousJob.getID()));
                vars.addAll(jobsVars.get(searchJob.getID()));
                jobsVars.put(unionJob.getID(), vars);
                searchJobs.add(searchJob);
                unionJobs.add(unionJob);
                previousJob = unionJob;
                previousJob = expandProperty(false, "INVERSE", previousJob, o, inverseOf.asNode(), s, depth + 1, searchJobs, unionJobs, joinJobs, jobsVars);
            }
        return previousJob;
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
