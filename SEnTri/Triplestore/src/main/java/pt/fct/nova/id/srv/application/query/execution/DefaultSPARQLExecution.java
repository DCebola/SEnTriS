package pt.fct.nova.id.srv.application.query.execution;

import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ResultSetStream;
import pt.fct.nova.id.srv.application.query.execution.exceptions.SPARQLExecutionException;
import pt.fct.nova.id.srv.application.query.jobs.Job;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.Job2;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.Job1;
import pt.fct.nova.id.srv.application.query.plans.QueryExecutionPlan;
import pt.fct.nova.id.srv.application.storage.iri_tables.IRITable;

import java.util.*;

public class DefaultSPARQLExecution implements SPARQLExecution {



    private final Map<String, Job> jobs;
    private final Map<String, IRITable> jobResults;
    private String current;
    private final Queue<String> pending;
    private final List<String> finished;
    private ResultSet result;
    private final List<Var> vars;


    public DefaultSPARQLExecution(QueryExecutionPlan plan) {
        this.vars = plan.getVars();
        this.jobs = plan.getJobs();
        this.pending = plan.getExecutionOrder();
        this.finished = new LinkedList<>();
        this.current = null;
        jobResults = new HashMap<>();
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
    public String getCurrentJob() {
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
        return result;
    }

    @Override
    public ResultSet exec(SPARQLWorker worker) throws SPARQLExecutionException{
        while (!pending.isEmpty()) {
            current = pending.peek();
            jobResults.put(current, delegateJob(worker, current));
            finished.add(pending.poll());
        }
        result = ResultSetStream.create(vars, worker.generateBindings(jobResults.get(current)).iterator());
        return result;
    }

    private IRITable delegateJob(SPARQLWorker worker, String current) throws SPARQLExecutionException {
        Job job = jobs.get(current);
        if (job instanceof Job1)
            return worker.exec((Job1) job,
                    jobResults.get(((Job1) job).getPrevJobID())
            );
        else if (job instanceof Job2)
            return worker.exec((Job2) job,
                    jobResults.get(((Job2) job).getLeftJobID()),
                    jobResults.get(((Job2) job).getRightJobID())
            );
        else
            return worker.exec(job);
    }
}
