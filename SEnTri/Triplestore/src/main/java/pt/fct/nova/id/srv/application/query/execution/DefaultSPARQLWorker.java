package pt.fct.nova.id.srv.application.query.execution;

import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Query;
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

public class DefaultSPARQLWorker implements SPARQLWorker {

    private final StorageEngine storageEngine;
    private final String triplestoreID;
    private final SPARQLResult result;

    public DefaultSPARQLWorker(String triplestoreID, StorageEngine storageEngine) {
        this.triplestoreID = triplestoreID;
        this.storageEngine = storageEngine;
        result = new DefaultSPARQLResult();
    }

    @Override
    public IRITable exec(Job job) throws SPARQLExecutionException {
        if (job instanceof SearchJob) return execSearch((SearchJob) job);
        else if (job instanceof EmptyResJob) return new MemIRITable(((EmptyResJob) job).getVars());
        throw new JobInstanceException(job.getClass().toString(), job.getID());
    }

    private IRITable execSearch(SearchJob job) throws SPARQLExecutionException {

        Node s = job.getSubject();
        Node p = job.getPredicate();
        Node o = job.getObject();

        try {
            return switch (job.getVariablesPattern()) {
                case S -> storageEngine.findS(triplestoreID, p, o, Var.alloc(s));
                case P -> storageEngine.findP(triplestoreID, s, o, Var.alloc(p));
                case O -> storageEngine.findO(triplestoreID, s, p, Var.alloc(o));
                case SP -> storageEngine.findSP(triplestoreID, o, Var.alloc(s), Var.alloc(p));
                case SO -> storageEngine.findSO(triplestoreID, p, Var.alloc(s), Var.alloc(o));
                case PO -> storageEngine.findPO(triplestoreID, s, Var.alloc(p), Var.alloc(o));
                case SPO -> throw new NotImplemented();
            };
        } catch (InvalidNodeException e) {
            throw new SPARQLExecutionException("Invalid node.");
        }
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
        throw new JobInstanceException(job.getClass().toString(), job.getID());

    }

    private IRITable execProject(ProjectJob job, IRITable prevJobResults) {
        prevJobResults.project(job.getVars());
        return prevJobResults;
    }

    private IRITable execOrderBy(OrderByJob job, IRITable prevJobResults) {
        result.setOrdered(true);
        result.setSortConditions(job.getSortConditions());
        return prevJobResults;
    }

    private IRITable execSlice(SliceJob job, IRITable prevJobResults) {
        result.setSliced(true);
        result.setLength(job.getLength());
        result.setOffset(job.getOffset());
        return prevJobResults;
    }

    private IRITable execDistinct(IRITable prevJobResults) {
        result.setDistinct(true);
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
        IRITable join = left.join(right);
        System.out.println("JOIN" + join.getPatterns().size());
        System.out.println("[L] -" + Arrays.toString(left.getVars().toArray()));
        System.out.println("[R] -" + Arrays.toString(right.getVars().toArray()));
        return join;
    }

    private IRITable execUnion(IRITable left, IRITable right) {
        IRITable union = left.union(right);
        System.out.println("UNION: " + union.getPatterns().size());
        System.out.println("[L] -" + Arrays.toString(left.getVars().toArray()));
        System.out.println("[R] -" + Arrays.toString(right.getVars().toArray()));
        return union;
    }

    private IRITable execOptional(IRITable left, IRITable right) {
        IRITable optional = left.leftOuterJoin(right);
        System.out.println("OPTIONAL: " + optional.getPatterns().size());
        System.out.println("[L] -" + Arrays.toString(left.getVars().toArray()));
        System.out.println("[R] -" + Arrays.toString(right.getVars().toArray()));
        return optional;
    }

    private IRITable execMinus(IRITable left, IRITable right) {
        IRITable minus = left.minus(right);
        System.out.println("MINUS: " + minus.getPatterns().size());
        System.out.println("[L] -" + Arrays.toString(left.getVars().toArray()));
        System.out.println("[R] -" + Arrays.toString(right.getVars().toArray()));
        return minus;
    }

    @Override
    public SPARQLResult generateResults(IRITable jobResults) {
        Collection<SerializableBinding> serializableBindings;
        boolean isDistinct = result.isDistinct();
        boolean isOrdered = result.isOrdered();

        if (isOrdered) {
            List<SerializableSortCondition> serializableSortConditions = result.getSortConditions();
            List<SortCondition> sortConditions = new ArrayList<>(serializableSortConditions.size());
            Collection<Binding> bindings;
            for (SerializableSortCondition condition : serializableSortConditions)
                sortConditions.add(new SortCondition(condition.getVar(), condition.getDir()));
            if (isDistinct)
                bindings = generateBindings(new TreeSet<>(new BindingComparator(sortConditions)), jobResults);
            else
                bindings = generateBindings(new LinkedList<>(), jobResults).stream().sorted(new BindingComparator(sortConditions)).collect(Collectors.toList());
            serializableBindings = generateSerializableBindings(bindings);
        } else {
            if (isDistinct)
                serializableBindings = generateSerializableBindings(new HashSet<>(), jobResults);
            else
                serializableBindings = generateSerializableBindings(new LinkedList<>(), jobResults);
        }
        if (result.isSliced()) {
            long offset = result.getOffset();
            long length = result.getLength();
            if (offset != Query.NOLIMIT && length != Query.NOLIMIT)
                serializableBindings = serializableBindings.stream().skip(offset).limit(length).collect(Collectors.toList());
            else if (offset != Query.NOLIMIT)
                serializableBindings = serializableBindings.stream().skip(offset).collect(Collectors.toList());
            else if (length != Query.NOLIMIT)
                serializableBindings = serializableBindings.stream().limit(length).collect(Collectors.toList());
        }
        result.setBindings(serializableBindings);
        return result;
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

    private Collection<SerializableBinding> generateSerializableBindings(Collection<Binding> bindings) {
        List<SerializableBinding> serializableBindings = new ArrayList<>(bindings.size());
        HashMap<Var, String> values;
        for (Binding binding : bindings) {
            values = new HashMap<>();
            for (Iterator<Var> it = binding.vars(); it.hasNext(); ) {
                Var var = it.next();
                values.put(var, binding.get(var).getURI());
            }
            serializableBindings.add(new SerializableBinding(values));
        }
        return serializableBindings;
    }

    private Collection<SerializableBinding> generateSerializableBindings(Collection<SerializableBinding> bindings, IRITable jobResults) {
        List<Var> vars = new ArrayList<>(jobResults.getVars());
        int i;
        HashMap<Var, String> values;
        for (List<String> p_values : jobResults.getPatterns()) {
            values = new HashMap<>();
            i = 0;
            for (String val : p_values) {
                if (val != null)
                    values.put(vars.get(i), val);
                i++;
            }
            bindings.add(new SerializableBinding(values));
        }
        return bindings;
    }
}
