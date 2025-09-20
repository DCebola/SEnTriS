package pt.fct.nova.id.srv.application.query.execution;

import org.apache.jena.sparql.core.Var;
import pt.fct.nova.id.srv.application.crypto.dgk.DGKEqKey;
import pt.fct.nova.id.srv.application.query.execution.exceptions.JobInstanceException;
import pt.fct.nova.id.srv.application.query.execution.exceptions.SPARQLExecutionException;
import pt.fct.nova.id.srv.application.query.jobs.EmptyResJob;
import pt.fct.nova.id.srv.application.query.jobs.Job;
import pt.fct.nova.id.srv.application.query.jobs.SecureSearchJob;
import pt.fct.nova.id.srv.application.query.jobs.SerializableBinding;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.*;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.*;
import pt.fct.nova.id.srv.application.storage.redis.ProxyStorageV2;
import pt.fct.nova.id.srv.application.storage.tables.BindingsTableV1;
import pt.fct.nova.id.srv.application.storage.tables.MemBindingsTableV1;

import static pt.fct.nova.id.srv.application.Utils.generateID;
import java.util.*;

public class SecureSPARQLWorkerV2 implements SPARQLWorkerV2 {

    private final SPARQLResult<byte[]> result;
    private final DGKEqKey key;
    private final Set<String> allSearchIDs;
    private final byte[] executionID;

    public SecureSPARQLWorkerV2(DGKEqKey key) {
        result = new DefaultSPARQLResult<>();
        this.key = key;
        allSearchIDs = new HashSet<>();
        executionID = generateID();
    }

    public Set<String> getAllSearchIDs() {
        return allSearchIDs;
    }

    @Override
    public BindingsTableV1 exec(Job job) throws SPARQLExecutionException {
        if (job instanceof SecureSearchJob secureSearchJob) {
            Map<Var, String> searches = secureSearchJob.getSearches();
            allSearchIDs.addAll(searches.values());
            return ProxyStorageV2.search(key, secureSearchJob.getVars(), searches, executionID);
        } else if (job instanceof EmptyResJob) return new MemBindingsTableV1(((EmptyResJob) job).getVars());
        throw new JobInstanceException(job.getClass().toString(), job.getID());
    }

    @Override
    public BindingsTableV1 exec(Job1 job, BindingsTableV1 prevJobResults) {
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

    private BindingsTableV1 execProject(ProjectJob job, BindingsTableV1 prevJobResults) {
        System.out.println("PROJECT BEFORE: " + prevJobResults.getVars() + " | " + prevJobResults.getPatterns().size());
        prevJobResults.project(job.getVars());
        System.out.println("PROJECT AFTER: " + prevJobResults.getVars() + " | " + prevJobResults.getPatterns().size());
        return prevJobResults;
    }

    private BindingsTableV1 execOrderBy(OrderByJob job, BindingsTableV1 prevJobResults) {
        result.setOrdered(true);
        result.setSortConditions(job.getSortConditions());
        return prevJobResults;
    }

    private BindingsTableV1 execSlice(SliceJob job, BindingsTableV1 prevJobResults) {
        result.setSliced(true);
        result.setLength(job.getLength());
        result.setOffset(job.getOffset());
        return prevJobResults;
    }

    private BindingsTableV1 execDistinct(BindingsTableV1 prevJobResults) {
        result.setDistinct(true);
        return prevJobResults;
    }

    @Override
    public BindingsTableV1 exec(Job2 job, BindingsTableV1 left, BindingsTableV1 right) throws SPARQLExecutionException {
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

    private BindingsTableV1 execJoin(BindingsTableV1 left, BindingsTableV1 right) {
        BindingsTableV1 join = left.join(right);
        System.out.println("JOIN: " + join.getPatterns().size());
        System.out.println("[L] -" + Arrays.toString(left.getVars().toArray()));
        System.out.println("[R] -" + Arrays.toString(right.getVars().toArray()));
        return join;
    }

    private BindingsTableV1 execUnion(BindingsTableV1 left, BindingsTableV1 right) {
        BindingsTableV1 union = left.union(right);
        System.out.println("UNION: " + union.getPatterns().size());
        System.out.println("[L] -" + Arrays.toString(left.getVars().toArray()));
        System.out.println("[R] -" + Arrays.toString(right.getVars().toArray()));
        return union;
    }

    private BindingsTableV1 execOptional(BindingsTableV1 left, BindingsTableV1 right) {
        BindingsTableV1 optional = left.leftOuterJoin(right);
        System.out.println("OPTIONAL: " + optional.getPatterns().size());
        System.out.println("[L] -" + Arrays.toString(left.getVars().toArray()));
        System.out.println("[R] -" + Arrays.toString(right.getVars().toArray()));
        return optional;
    }

    private BindingsTableV1 execMinus(BindingsTableV1 left, BindingsTableV1 right) {
        BindingsTableV1 minus = left.minus(right);
        System.out.println("MINUS: " + minus.getPatterns().size());
        System.out.println("[L] -" + Arrays.toString(left.getVars().toArray()));
        System.out.println("[R] -" + Arrays.toString(right.getVars().toArray()));
        return minus;
    }

    @Override
    public SPARQLResult<byte[]> generateResults(BindingsTableV1 jobResults) {
        Collection<SerializableBinding<byte[]>> bindings;
        boolean isDistinct = result.isDistinct();
        if (isDistinct)
            bindings = generateBindings(new HashSet<>(), jobResults);
        else
            bindings = generateBindings(new LinkedList<>(), jobResults);
        result.setBindings(bindings);
        return result;
    }

    /*
    private Collection<SerializableBinding<byte[]>> generateBindings(Collection<SerializableBinding<byte[]>> bindings, BindingsTableV1 jobResults) {
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
    */


    private Collection<SerializableBinding<byte[]>> generateBindings(Collection<SerializableBinding<byte[]>> bindings, BindingsTableV1 jobResults) {
        List<Var> vars = new ArrayList<>(jobResults.getVars());
        int i;
        HashMap<Var, byte[]> values;
        Map<byte[], byte[]> eqTags = ProxyStorageV2.getEqTags(executionID);
        for (List<byte[]> p_values : jobResults.getPatterns()) {
            values = new HashMap<>();
            i = 0;
            for (byte[] val : p_values) {
                if (val != null)
                    values.put(vars.get(i), eqTags.get(val));
                i++;
            }
            bindings.add(new SerializableBinding<>(values));
        }
        ProxyStorageV2.deleteEqTags(executionID);
        return bindings;
    }
}
