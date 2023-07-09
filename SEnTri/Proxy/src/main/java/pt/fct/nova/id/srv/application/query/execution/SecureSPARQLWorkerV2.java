package pt.fct.nova.id.srv.application.query.execution;

import org.apache.jena.sparql.core.Var;
import pt.fct.nova.id.srv.application.crypto.dgk.DGKEqKey;
import pt.fct.nova.id.srv.application.crypto.dgk.HomomorphicException;
import pt.fct.nova.id.srv.application.query.execution.exceptions.JobInstanceException;
import pt.fct.nova.id.srv.application.query.execution.exceptions.SPARQLExecutionException;
import pt.fct.nova.id.srv.application.query.jobs.EmptyResJob;
import pt.fct.nova.id.srv.application.query.jobs.Job;
import pt.fct.nova.id.srv.application.query.jobs.SecureSearchJob;
import pt.fct.nova.id.srv.application.query.jobs.SerializableBinding;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.*;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.*;
import pt.fct.nova.id.srv.application.storage.redis.ProxyStorageV2;
import pt.fct.nova.id.srv.application.storage.tables.BindingsTableV2;
import pt.fct.nova.id.srv.application.storage.tables.MemBindingsTableV2;

import java.math.BigInteger;
import java.util.*;

public class SecureSPARQLWorkerV2 implements SPARQLWorkerV2 {

    private final SPARQLResult<byte[]> result;
    private final DGKEqKey key;
    private final Set<String> allSearchIDs;

    public SecureSPARQLWorkerV2(DGKEqKey key) {
        result = new DefaultSPARQLResult<>();
        this.key = key;
        allSearchIDs = new HashSet<>();
    }

    public Set<String> getAllSearchIDs() {
        return allSearchIDs;
    }

    @Override
    public BindingsTableV2 exec(Job job) throws SPARQLExecutionException {
        if (job instanceof SecureSearchJob secureSearchJob) {
            Map<Var, String> searches = secureSearchJob.getSearches();
            allSearchIDs.addAll(searches.values());
            return ProxyStorageV2.search(key, secureSearchJob.getVars(), searches);
        } else if (job instanceof EmptyResJob) return new MemBindingsTableV2(((EmptyResJob) job).getVars());
        throw new JobInstanceException(job.getClass().toString(), job.getID());
    }

    @Override
    public BindingsTableV2 exec(Job1 job, BindingsTableV2 prevJobResults) {
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

    private BindingsTableV2 execProject(ProjectJob job, BindingsTableV2 prevJobResults) {
        System.out.println("PROJECT BEFORE: " + prevJobResults.getVars() + " | " + prevJobResults.getPatterns().size());
        prevJobResults.project(job.getVars());
        System.out.println("PROJECT AFTER: " + prevJobResults.getVars() + " | " + prevJobResults.getPatterns().size());
        return prevJobResults;
    }

    private BindingsTableV2 execOrderBy(OrderByJob job, BindingsTableV2 prevJobResults) {
        result.setOrdered(true);
        result.setSortConditions(job.getSortConditions());
        return prevJobResults;
    }

    private BindingsTableV2 execSlice(SliceJob job, BindingsTableV2 prevJobResults) {
        result.setSliced(true);
        result.setLength(job.getLength());
        result.setOffset(job.getOffset());
        return prevJobResults;
    }

    private BindingsTableV2 execDistinct(BindingsTableV2 prevJobResults) {
        result.setDistinct(true);
        return prevJobResults;
    }

    @Override
    public BindingsTableV2 exec(Job2 job, BindingsTableV2 left, BindingsTableV2 right) throws SPARQLExecutionException {
        try {
            if (job instanceof JoinJob)
                return execJoin(left, right);
            else if (job instanceof UnionJob)
                return execUnion(left, right);
            else if (job instanceof OptionalJob)
                return execOptional(left, right);
            else if (job instanceof MinusJob)
                return execMinus(left, right);
        } catch (HomomorphicException e) {
            throw new SPARQLExecutionException(e.getMessage());
        }
        throw new JobInstanceException(job.getClass().toString(), job.getID());
    }

    private BindingsTableV2 execJoin(BindingsTableV2 left, BindingsTableV2 right) throws HomomorphicException {
        BindingsTableV2 join = left.join(right, key);
        System.out.println("JOIN: " + join.getPatterns().size());
        System.out.println("[L] -" + Arrays.toString(left.getVars().toArray()));
        System.out.println("[R] -" + Arrays.toString(right.getVars().toArray()));
        return join;
    }

    private BindingsTableV2 execUnion(BindingsTableV2 left, BindingsTableV2 right) {
        BindingsTableV2 union = left.union(right);
        System.out.println("UNION: " + union.getPatterns().size());
        System.out.println("[L] -" + Arrays.toString(left.getVars().toArray()));
        System.out.println("[R] -" + Arrays.toString(right.getVars().toArray()));
        return union;
    }

    private BindingsTableV2 execOptional(BindingsTableV2 left, BindingsTableV2 right) throws HomomorphicException {
        BindingsTableV2 optional = left.leftOuterJoin(right, key);
        System.out.println("OPTIONAL: " + optional.getPatterns().size());
        System.out.println("[L] -" + Arrays.toString(left.getVars().toArray()));
        System.out.println("[R] -" + Arrays.toString(right.getVars().toArray()));
        return optional;
    }

    private BindingsTableV2 execMinus(BindingsTableV2 left, BindingsTableV2 right) throws HomomorphicException {
        BindingsTableV2 minus = left.minus(right, key);
        System.out.println("MINUS: " + minus.getPatterns().size());
        System.out.println("[L] -" + Arrays.toString(left.getVars().toArray()));
        System.out.println("[R] -" + Arrays.toString(right.getVars().toArray()));
        return minus;
    }

    @Override
    public SPARQLResult<byte[]> generateResults(BindingsTableV2 jobResults) {
        Collection<SerializableBinding<byte[]>> bindings;
        boolean isDistinct = result.isDistinct();
        if (isDistinct)
            bindings = generateBindings(new HashSet<>(), jobResults);
        else
            bindings = generateBindings(new LinkedList<>(), jobResults);
        result.setBindings(bindings);
        return result;
    }

    private Collection<SerializableBinding<byte[]>> generateBindings(Collection<SerializableBinding<byte[]>> bindings, BindingsTableV2 jobResults) {
        List<Var> vars = new ArrayList<>(jobResults.getVars());
        int i;
        HashMap<Var, byte[]> values;
        for (List<BigInteger> p_values : jobResults.getPatterns()) {
            values = new HashMap<>();
            i = 0;
            for (BigInteger val : p_values) {
                if (val != null)
                    values.put(vars.get(i), val.toByteArray());
                i++;
            }
            bindings.add(new SerializableBinding<>(values));
        }
        return bindings;
    }
}
