package pt.fct.nova.id.srv.application.query.execution;

import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingBuilder;
import org.apache.jena.sparql.engine.binding.BindingComparator;
import pt.fct.nova.id.srv.application.query.execution.exceptions.*;
import pt.fct.nova.id.srv.application.query.jobs.*;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.*;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.*;
import pt.fct.nova.id.srv.application.storage.EncryptedStorageEngine;

import pt.fct.nova.id.srv.application.storage.iri_tables.IRITable;
import pt.fct.nova.id.srv.application.storage.iri_tables.MemIRITable;

import java.util.*;
import java.util.stream.Collectors;


public class SecureSPARQLWorker implements SPARQLWorker {
    private final EncryptedStorageEngine storageEngine;
    private final String storeID;
    private final SPARQLResultType resultType;

    public SecureSPARQLWorker(String storeID, EncryptedStorageEngine storageEngine) {
        this.storeID = storeID;
        this.storageEngine = storageEngine;
        resultType = new SimpleSPARQLResultType();
    }

    @Override
    public IRITable exec(Job job) throws SPARQLExecutionException {
        if (job instanceof SecureSearchJob) return execSearch((SecureSearchJob) job);
        //else if (job instanceof ValuesJob) return execValues((ValuesJob) job);
        else if (job instanceof EmptyResJob) return new MemIRITable(((EmptyResJob) job).getVars());
        throw new JobInstanceException(job.getClass().toString(), job.getID());
    }

    private IRITable execSearch(SecureSearchJob job) {
        List<Var> vars = job.getVars();
        int numVars = vars.size();
        if (numVars > 2 || numVars == 0)
            throw new SecureSearchException(job.getClass().toString(), job.getID(), numVars);
        else {
            if (numVars == 2)
                return storageEngine.search(storeID, vars.get(0), vars.get(1), job.getTrapdoors());
            else
                return storageEngine.search(storeID, vars.get(0), job.getTrapdoors());
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
        resultType.setOrdered(true);
        resultType.setSortConditions(job.getSortConditions());
        return prevJobResults;
    }

    private IRITable execSlice(SliceJob job, IRITable prevJobResults) {
        resultType.setSliced(true);
        resultType.setLength(job.getLength());
        resultType.setOffset(job.getOffset());
        return prevJobResults;
    }

    private IRITable execDistinct(IRITable prevJobResults) {
        resultType.setDistinct(true);
        return prevJobResults;
    }

    private IRITable execBind(BindJob job, IRITable prevJobResults) {
        //TODO Execute BindJob
        throw new JobNotImplementedException(job.getClass().toString(), job.getID());
    }

    private IRITable execFilter(FilterJob job, IRITable prevJobResults) {
        //TODO Execute FilterJob
        throw new JobNotImplementedException(job.getClass().toString(), job.getID());
    }

    private IRITable execGroup(GroupJob job, IRITable prevJobResults) {
        //TODO Execute GroupJob
        throw new JobNotImplementedException(job.getClass().toString(), job.getID());
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
        boolean isDistinct = resultType.isDistinct();
        boolean isOrdered = resultType.isOrdered();
        if (isDistinct && isOrdered)
            res = generateBindings(new TreeSet<>(new BindingComparator(resultType.getSortConditions())), jobResults);
        else if (isDistinct)
            res = generateBindings(new HashSet<>(), jobResults);
        else {
            res = generateBindings(new LinkedList<>(), jobResults);
            if (isOrdered)
                res = res.stream().sorted(new BindingComparator(resultType.getSortConditions())).collect(Collectors.toList());
        }
        if (resultType.isSliced()) {
            long offset = resultType.getOffset();
            long length = resultType.getLength();
            if (offset != Query.NOLIMIT && length != Query.NOLIMIT)
                res = res.stream().skip(offset).limit(length).collect(Collectors.toList());
            else if (offset != Query.NOLIMIT)
                res = res.stream().skip(offset).collect(Collectors.toList());
            else if (length != Query.NOLIMIT)
                res = res.stream().limit(length).collect(Collectors.toList());
        }
        return res;
    }

    private Collection<Binding> generateBindings(Collection<Binding> bindings, IRITable jobResults) {
        List<Var> vars = new ArrayList<>(jobResults.getVars());
        BindingBuilder builder = Binding.builder();
        int i;
        for (List<String> p_values : jobResults.getPatterns()) {
            i = 0;
            for (String val : p_values) {
                if (val != null)
                    builder.add(vars.get(i), NodeFactory.createURI(val));
                i++;
            }
            bindings.add(builder.build());
            builder.reset();
        }
        return bindings;
    }
}
