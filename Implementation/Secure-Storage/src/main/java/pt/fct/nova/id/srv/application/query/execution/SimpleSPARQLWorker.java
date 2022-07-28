package pt.fct.nova.id.srv.application.query.execution;

import org.apache.jena.graph.Node;
import org.apache.jena.query.SortCondition;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingBuilder;
import org.apache.jena.sparql.engine.binding.BindingComparator;
import pt.fct.nova.id.srv.application.query.execution.exceptions.*;
import pt.fct.nova.id.srv.application.query.jobs.*;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.*;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.*;
import pt.fct.nova.id.srv.application.storage.StorageEngine;
import pt.fct.nova.id.srv.application.storage.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.storage.iri_tables.IRITable;
import pt.fct.nova.id.srv.application.storage.iri_tables.MemIRITable;

import java.util.*;
import java.util.stream.Collectors;

import static pt.fct.nova.id.srv.application.Utils.generateID;
import static pt.fct.nova.id.srv.application.query.jobs.VariablesPattern.*;

public class SimpleSPARQLWorker implements SPARQLWorker {

    private final StorageEngine storageEngine;
    private final String storeID;

    private List<SortCondition> sortConditions;

    private boolean isDistinct;
    private boolean isOrdered;
    private boolean isSliced;
    private long offset;
    private long length;


    public SimpleSPARQLWorker(String storeID, StorageEngine storageEngine) {
        this.storeID = storeID;
        this.storageEngine = storageEngine;
        this.isDistinct = false;
        this.isOrdered = false;
        this.isSliced = false;
        this.offset = 0;
        this.length = Long.MAX_VALUE;
    }

    @Override
    public IRITable exec(Job job) throws SPARQLExecutionException {
        if (job instanceof GetJob) return execGet((GetJob) job);
        else if (job instanceof EmptyResJob) return new MemIRITable(((EmptyResJob) job).getVars());
        else if (job instanceof ValuesJob) return execValues((ValuesJob) job);
        throw new JobInstanceException(job.getClass().toString(), job.getID());
    }

    private IRITable execGet(GetJob job) throws SPARQLExecutionException {
        Node s = job.getSubject();
        Node p = job.getPredicate();
        Node o = job.getObject();
        VariablesPattern vp = job.getVariablesPattern();
        IRITable res = switch (vp) {
            case S -> retrieveGetResults(S, Var.alloc(s), p, o);
            case P -> retrieveGetResults(P, Var.alloc(p), s, o);
            case O -> retrieveGetResults(O, Var.alloc(o), s, p);
            case SP -> retrieveGetResults(SP, Var.alloc(s), Var.alloc(p), o);
            case SO -> retrieveGetResults(SO, Var.alloc(s), Var.alloc(o), p);
            case PO -> retrieveGetResults(PO, Var.alloc(p), Var.alloc(o), s);
            case SPO -> storageEngine.findAll(storeID, Var.alloc(s), Var.alloc(p), Var.alloc(o));
        };
        if (res == null)
            throw new GetJobPatternException(job.getClass().toString(), job.getID(), job.getVariablesPattern());
        return res;
    }

    private IRITable retrieveGetResults(VariablesPattern varPattern, Var var, Node node1, Node node2) {
        if (varPattern == VariablesPattern.S) return storageEngine.findSubjects(storeID, node1, node2, var);
        else if (varPattern == VariablesPattern.P) return storageEngine.findPredicates(storeID, node1, node2, var);
        else if (varPattern == VariablesPattern.O) return storageEngine.findObjects(storeID, node1, node2, var);
        return null;
    }

    private IRITable retrieveGetResults(VariablesPattern varPattern, Var var1, Var var2, Node node) {
        if (varPattern == VariablesPattern.SP)
            return storageEngine.findSP(storeID, node, var1, var2);
        else if (varPattern == VariablesPattern.SO)
            return storageEngine.findSO(storeID, node, var1, var2);
        else if (varPattern == VariablesPattern.PO)
            return storageEngine.findPO(storeID, node, var1, var2);
        return null;
    }

    private IRITable execValues(ValuesJob job) {
        IRITable res = new MemIRITable();
        Var var;
        Node node;
        Iterator<Var> vars;
        for (Binding binding : job.getValues()) {
            String p_idx = generateID();
            vars = binding.vars();
            while (vars.hasNext()) {
                var = vars.next();
                node = binding.get(var);
                try {
                    res.add(p_idx, var, storageEngine.parseNodeIRI(node));
                } catch (InvalidNodeException e) {
                    e.printStackTrace();
                    throw new ValuesNodeException(job.getClass().toString(), job.getID(), node);
                }
            }
        }
        return res;
    }

    @Override
    public IRITable exec(Job1 job, IRITable prevJobResults) {
        if (job instanceof ProjectJob)
            return execProject((ProjectJob) job, prevJobResults);
        else if (job instanceof OrderByJob)
            return execOrderBy((OrderByJob) job, prevJobResults);
        else if (job instanceof DistinctJob)
            return execDistinct(prevJobResults);
        else if (job instanceof SliceJob)
            return execSlice((SliceJob) job, prevJobResults);
        else if (job instanceof GroupJob)
            return execGroup((GroupJob) job, prevJobResults);
        else if (job instanceof BindJob)
            return execBind((BindJob) job, prevJobResults);
        else if (job instanceof FilterJob)
            return execFilter((FilterJob) job, prevJobResults);
        throw new JobInstanceException(job.getClass().toString(), job.getID());

    }

    private IRITable execProject(ProjectJob job, IRITable prevJobResults) {
        prevJobResults.project(job.getVariables());
        return prevJobResults;
    }

    private IRITable execOrderBy(OrderByJob job, IRITable prevJobResults) {
        isOrdered = true;
        List<SortCondition> conditions = job.getSortConditions();
        if (sortConditions != null)
            sortConditions.addAll(conditions);
        else
            sortConditions = job.getSortConditions();
        return prevJobResults;
    }

    private IRITable execSlice(SliceJob job, IRITable prevJobResults) {
        isSliced = true;
        offset = job.getOffset();
        length = job.getLength();
        return prevJobResults;
    }

    private IRITable execDistinct(IRITable prevJobResults) {
        isDistinct = true;
        return prevJobResults;
    }

    private IRITable execBind(BindJob job, IRITable prevJobResults) {
        //TODO Execute BindJob
        throw new JobNotImplementedException(job.getClass().toString(), job.getID());
    }

    private IRITable execFilter(FilterJob job, IRITable prevJobResults) {
        //TODO Execute FilterJob
        return prevJobResults;
    }

    private IRITable execGroup(GroupJob job, IRITable prevJobResults) {
        //TODO Execute GroupJob
        return prevJobResults;
    }

    @Override
    public IRITable exec(Job2 job, IRITable left, IRITable right) throws SPARQLExecutionException {
        if (job instanceof JoinJob)
            return execJoin(left, right);
        else if (job instanceof UnionJob)
            return execUnion(left, right);
        else if (job instanceof OptionalJob)
            return execOptional(left, right);
        else if (job instanceof MinusJob)
            return execMinus(left, right);
        throw new JobInstanceException(job.getClass().toString(), job.getID());
    }

    private IRITable execJoin(IRITable left, IRITable right) {
        return left.join(right);
    }

    private IRITable execUnion(IRITable left, IRITable right) {
        return left.union(right);
    }

    private IRITable execOptional(IRITable left, IRITable right) {
        return left.leftOuterJoin(right);
    }

    private IRITable execMinus(IRITable left, IRITable right) {
        return left.minus(right);
    }

    @Override
    public Collection<Binding> generateBindings(IRITable jobResults) {
        Collection<Binding> res;
        if (isDistinct && isOrdered)
            res = generateBindings(new TreeSet<>(new BindingComparator(sortConditions)), jobResults);
        else if (isDistinct)
            res = generateBindings(new HashSet<>(), jobResults);
        else {
            res = generateBindings(new LinkedList<>(), jobResults);
            if (isOrdered)
                res = res.stream().sorted(new BindingComparator(sortConditions)).collect(Collectors.toList());
        }
        if (isSliced)
            res = res.stream().skip(offset).limit(length).collect(Collectors.toList());
        return res;
    }

    private Collection<Binding> generateBindings(Collection<Binding> bindings, IRITable jobResults) {
        List<Var> vars = new ArrayList<>(jobResults.getVars());
        BindingBuilder builder = Binding.builder();
        int i;
        for (List<String> p_iris : jobResults.getPatterns()) {
            i = 0;
            for (String iri : p_iris) {
                if (iri != null)
                    builder.add(vars.get(i), storageEngine.generateNode(iri));
                i++;
            }
            bindings.add(builder.build());
            builder.reset();
        }
        return bindings;
    }
}
