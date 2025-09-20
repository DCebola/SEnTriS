package pt.fct.nova.id.srv.application.query.execution;

import org.apache.jena.sparql.core.Var;
import pt.fct.nova.id.srv.application.query.execution.exceptions.*;
import pt.fct.nova.id.srv.application.query.jobs.*;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.*;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.*;
import pt.fct.nova.id.srv.application.storage.redis.ProxyStorageV1;
import pt.fct.nova.id.srv.application.storage.tables.BindingsTable;
import pt.fct.nova.id.srv.application.storage.tables.MemBindingsTable;

import javax.crypto.SecretKey;
import java.util.*;

public class SecureSPARQLWorkerV1 extends SecureSPARQLWorker {

    private final SecretKey key;

    public SecureSPARQLWorkerV1(SecretKey key) {
        super();
        this.key = key;
    }

    @Override
    public BindingsTable exec(Job job) throws SPARQLExecutionException {
        if (job instanceof SecureSearchJob secureSearchJob) {
            Map<Var, String> searches = secureSearchJob.getSearches();
            this.getAllSearchIDs().addAll(searches.values());
            return ProxyStorageV1.search(key, secureSearchJob.getVars(), searches);
        } else if (job instanceof EmptyResJob) return new MemBindingsTable(((EmptyResJob) job).getVars());
        throw new JobInstanceException(job.getClass().toString(), job.getID());
    }

    @Override
    public SPARQLResult<byte[]> generateResults(BindingsTable jobResults) {
        Collection<SerializableBinding<byte[]>> bindings;
        SPARQLResult<byte[]> result = this.getResult();
        boolean isDistinct = result.isDistinct();
        if (isDistinct)
            bindings = generateBindings(new HashSet<>(), jobResults);
        else
            bindings = generateBindings(new LinkedList<>(), jobResults);
        result.setBindings(bindings);
        return result;
    }

    private Collection<SerializableBinding<byte[]>> generateBindings(Collection<SerializableBinding<byte[]>> bindings, BindingsTable jobResults) {
        List<Var> vars = new ArrayList<>(jobResults.getVars());
        int i;
        HashMap<Var, byte[]> values;
        for (List<byte[]> p_values : jobResults.getPatterns()) {
            values = new HashMap<>();
            i = 0;
            for (byte[] val : p_values) {
                if (val != null)
                    values.put(vars.get(i), val);
                i++;
            }
            bindings.add(new SerializableBinding<>(values));
        }
        return bindings;
    }
}
