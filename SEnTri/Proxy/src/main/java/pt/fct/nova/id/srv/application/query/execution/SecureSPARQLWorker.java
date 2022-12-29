package pt.fct.nova.id.srv.application.query.execution;

import org.apache.jena.sparql.core.Var;
import pt.fct.nova.id.srv.application.query.execution.exceptions.*;
import pt.fct.nova.id.srv.application.query.jobs.*;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.*;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.*;
import pt.fct.nova.id.srv.application.storage.iri_tables.IRITable;
import pt.fct.nova.id.srv.application.storage.iri_tables.MemIRITable;
import pt.fct.nova.id.srv.application.storage.redis.ProxyStorage;

import javax.crypto.SecretKey;
import java.util.*;

public class SecureSPARQLWorker implements SPARQLWorker {

    private final SPARQLResult result;
    private final SecretKey key;
    private final Set<String> allSearchIDs;

    public SecureSPARQLWorker(SecretKey key) {
        result = new DefaultSPARQLResult();
        this.key = key;
        allSearchIDs = new HashSet<>();
    }

    public Set<String> getAllSearchIDs() {
        return allSearchIDs;
    }

    @Override
    public IRITable exec(Job job) throws SPARQLExecutionException {
        if (job instanceof SecureSearchJob secureSearchJob) {
            Map<Var, String> searches = secureSearchJob.getSearches();
            allSearchIDs.addAll(searches.values());
            return ProxyStorage.search(key, secureSearchJob.getVars(), searches);
        } else if (job instanceof EmptyResJob) return new MemIRITable(((EmptyResJob) job).getVars());
        throw new JobInstanceException(job.getClass().toString(), job.getID());
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
        System.out.println("PROJECT: " + prevJobResults.getVars() + " | " + prevJobResults.getPatterns().size());
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
        System.out.println("JOIN: " + join.getPatterns().size());
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
        Collection<SerializableBinding> bindings;
        boolean isDistinct = result.isDistinct();
        if (isDistinct)
            bindings = generateBindings(new HashSet<>(), jobResults);
        else
            bindings = generateBindings(new LinkedList<>(), jobResults);
        result.setBindings(bindings);
        return result;
    }

    private Collection<SerializableBinding> generateBindings(Collection<SerializableBinding> bindings, IRITable jobResults) {
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
