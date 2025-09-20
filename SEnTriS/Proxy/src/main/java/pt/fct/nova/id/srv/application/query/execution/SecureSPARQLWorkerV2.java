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
import pt.fct.nova.id.srv.application.storage.tables.BindingsTable;
import pt.fct.nova.id.srv.application.storage.tables.MemBindingsTable;

import static pt.fct.nova.id.srv.application.Utils.generateID;
import java.util.*;

public class SecureSPARQLWorkerV2 extends SecureSPARQLWorker {

    private final DGKEqKey key;
    private final byte[] executionID;

    public SecureSPARQLWorkerV2(DGKEqKey key) {
        super();
        this.key = key;
        executionID = generateID();
    }

    @Override
    public BindingsTable exec(Job job) throws SPARQLExecutionException {
        if (job instanceof SecureSearchJob secureSearchJob) {
            Map<Var, String> searches = secureSearchJob.getSearches();
            this.getAllSearchIDs().addAll(searches.values());
            return ProxyStorageV2.search(key, secureSearchJob.getVars(), searches, executionID);
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
