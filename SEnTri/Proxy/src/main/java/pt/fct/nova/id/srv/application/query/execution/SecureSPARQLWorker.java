package pt.fct.nova.id.srv.application.query.execution;

import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.SortCondition;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingBuilder;
import pt.fct.nova.id.srv.application.query.execution.exceptions.*;
import pt.fct.nova.id.srv.application.query.jobs.*;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.*;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.*;
import pt.fct.nova.id.srv.application.storage.iri_tables.IRITable;
import pt.fct.nova.id.srv.application.storage.iri_tables.MemIRITable;
import pt.fct.nova.id.srv.application.storage.iri_tables.MemValuesTable;
import pt.fct.nova.id.srv.application.storage.redis.ProxyStorage;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.stream.Collectors;

import static pt.fct.nova.id.srv.application.Utils.generateID;

public class SecureSPARQLWorker implements SPARQLWorker {

    private final SPARQLResult result;
    private final SecretKey key;

    public SecureSPARQLWorker(SecretKey key) {
        result = new DefaultSPARQLResult();
        this.key = key;
    }

    @Override
    public IRITable exec(Job job) throws SPARQLExecutionException{
        if (job instanceof SecureSearchJob) return ProxyStorage.search(key, ((SecureSearchJob) job).getVars());
        else if (job instanceof EncryptedValuesJob) return execSecureValues((EncryptedValuesJob) job);
        else if (job instanceof EmptyResJob) return new MemIRITable(((EmptyResJob) job).getVars());
        throw new JobInstanceException(job.getClass().toString(), job.getID());
    }

    private IRITable execSecureValues(EncryptedValuesJob job) {
        IRITable res = new MemValuesTable();
        Var var;
        String encryptedNode;
        Iterator<Var> vars;
        for (EncryptedBinding binding : job.getValues()) {
            String p_idx = generateID();
            vars = binding.vars();
            while (vars.hasNext()) {
                var = vars.next();
                encryptedNode = binding.get(var);
                res.add(p_idx, var, encryptedNode);
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
        result.setOrdered(true);
        List<SerializableSortCondition> serializableSortConditions = job.getSortConditions();
        List<SortCondition> sortConditions = new ArrayList<>(serializableSortConditions.size());
        for (SerializableSortCondition condition : serializableSortConditions)
            sortConditions.add(new SortCondition(condition.getVar(), condition.getDir()));
        result.setSortConditions(sortConditions);
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
    public SPARQLResult generateResults(IRITable jobResults) {
        Collection<Binding> bindings;
        boolean isDistinct = result.isDistinct();
        if (isDistinct)
            bindings = generateBindings(new HashSet<>(), jobResults);
        else
            bindings = generateBindings(new LinkedList<>(), jobResults);

        if (result.isSliced()) {
            long offset = result.getOffset();
            long length = result.getLength();
            if (offset != Query.NOLIMIT && length != Query.NOLIMIT)
                bindings = bindings.stream().skip(offset).limit(length).collect(Collectors.toList());
            else if (offset != Query.NOLIMIT)
                bindings = bindings.stream().skip(offset).collect(Collectors.toList());
            else if (length != Query.NOLIMIT)
                bindings = bindings.stream().limit(length).collect(Collectors.toList());
        }
        result.setBindings(bindings);
        return result;
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
