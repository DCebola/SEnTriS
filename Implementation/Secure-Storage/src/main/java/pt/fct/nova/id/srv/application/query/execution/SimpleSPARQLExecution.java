package pt.fct.nova.id.srv.application.query.execution;

import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ResultSetStream;
import org.apache.jena.sparql.engine.binding.Binding;
import pt.fct.nova.id.srv.application.query.jobs.Job;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.Job2;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.Job1;
import pt.fct.nova.id.srv.application.query.plans.QueryExecutionPlan;
import pt.fct.nova.id.srv.application.storage.StorageEngine;

import java.util.*;

public class SimpleSPARQLExecution implements SPARQLExecution {

    private final Map<String, Job> jobs;
    private String current;
    private final Queue<String> pending;
    private final List<String> finished;
    private final Map<String, List<Binding>> jobBindings;
    private final List<Var> vars;


    public SimpleSPARQLExecution(QueryExecutionPlan plan) {
        this.vars = plan.getVars();
        this.jobs = plan.getJobs();
        this.pending = plan.getExecutionOrder();
        this.finished = new LinkedList<>();
        this.current = null;
        jobBindings = new HashMap<>();
    }

    @Override
    public Iterator<String> getPendingJobs() {
        return pending.iterator();
    }

    @Override
    public Iterator<String> getFinishedJobs() {
        return finished.iterator();
    }

    @Override
    public String getCurrentJobs() {
        return current;
    }

    @Override
    public boolean isFinished() {
        return pending.isEmpty();
    }

    @Override
    public boolean isFinished(String jobID) {
        return finished.contains(jobID);
    }

    @Override
    public ResultSet getResults() {
        List<Binding> bindings_collector = new LinkedList<>();
        jobBindings.values().forEach(bindings_collector::addAll);
        return ResultSetStream.create(vars, bindings_collector.iterator());
    }

    @Override
    public ResultSet getResults(String jobID) {
        return ResultSetStream.create(vars, jobBindings.get(jobID).iterator());
    }

    @Override
    public ResultSet exec(String storeID, StorageEngine engine) {
        SPARQLWorker worker = new SimpleSPARQLWorker(storeID, engine);
        List<Binding> res;
        while (!pending.isEmpty()) {
            current = pending.peek();
            res = delegateJob(worker, current);
            if (res != null) {
                res.forEach(System.out::println);
                jobBindings.put(current, res);
            }
            finished.add(pending.poll());
        }
        return getResults();
    }

    private List<Binding> delegateJob(SPARQLWorker worker, String current) {
        Job job = jobs.get(current);
        if (job instanceof Job1) {
            return worker.exec((Job1) job,
                    jobBindings.get(((Job1) job).getPrevJobID())
            );
        } else if (job instanceof Job2) {
            return worker.exec((Job2) job,
                    jobBindings.get(((Job2) job).getLeftJobID()),
                    jobBindings.get(((Job2) job).getRightJobID())
            );
        } else {
            return worker.exec(job);
        }
    }
}
