package pt.fct.nova.id.srv.application.query.execution;

import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ResultSetStream;
import pt.fct.nova.id.srv.application.query.jobs.Job;
import pt.fct.nova.id.srv.application.query.jobs.jobN.JobN;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.Job2;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.Job1;
import pt.fct.nova.id.srv.application.query.plans.QueryExecutionPlan;
import pt.fct.nova.id.srv.application.storage.StorageEngine;
import pt.fct.nova.id.srv.application.storage.idx_data_structs.IdxTable;

import java.util.*;

public class SimpleSPARQLExecution implements SPARQLExecution {

    private final Map<String, Job> jobs;
    private final Map<String, IdxTable> jobResults;
    private String current;
    private final Queue<String> pending;
    private final List<String> finished;
    private ResultSet result;
    private final List<Var> vars;


    public SimpleSPARQLExecution(QueryExecutionPlan plan) {
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
    public ResultSet exec(String storeID, StorageEngine engine) {
        SPARQLWorker worker = new SimpleSPARQLWorker(storeID, engine);
        while (!pending.isEmpty()) {
            current = pending.peek();
            System.out.println("Current:" + current);
            jobResults.put(current, delegateJob(worker, current));
            finished.add(pending.poll());
        }
        result = ResultSetStream.create(vars, worker.generateBindings(jobResults.get(current)).iterator());
        return result;
    }

    private IdxTable delegateJob(SPARQLWorker worker, String current) {
        Job job = jobs.get(current);
        if (job instanceof Job1) {
            System.out.println("Previous Job:" + ((Job1) job).getPrevJobID());
            return worker.exec((Job1) job,
                    jobResults.get(((Job1) job).getPrevJobID())
            );
        } else if (job instanceof Job2) {
            System.out.println("Left:" + ((Job2) job).getLeftJobID());
            System.out.println("Right:" + ((Job2) job).getRightJobID());
            return worker.exec((Job2) job,
                    jobResults.get(((Job2) job).getLeftJobID()),
                    jobResults.get(((Job2) job).getRightJobID())
            );
        } else if (job instanceof JobN) {
            System.out.println("Previous Jobs:" + ((JobN) job).getPreviousJobIDs());
            List<String> prev_ids = ((JobN) job).getPreviousJobIDs();
            List<IdxTable> prevResults = new ArrayList<>(prev_ids.size());
            prev_ids.forEach(jobID -> prevResults.add(jobResults.get(jobID)));
            return worker.exec(((JobN) job), prevResults);
        } else {
            return worker.exec(job);
        }
    }
}
