package pt.fct.nova.id.srv.application.query.execution;

import org.apache.jena.query.ResultSet;
import pt.fct.nova.id.srv.application.query.jobs.Job;
import pt.fct.nova.id.srv.application.query.plans.ExecutionPlan;

import java.util.*;

public class SimpleSPARQLExecution implements SPARQLExecution {

    private final Map<Job, String> jobs;
    private final List<String> current;
    private final Queue<String> pending;
    private final List<String> finished;
    private final ResultSet result;


    public SimpleSPARQLExecution(ExecutionPlan plan) {
        this.jobs = plan.getJobs();
        this.pending = plan.getExecutionOrder();
        this.finished = new LinkedList<>();
        this.current = new LinkedList<>();
        result = null; //TODO create ResultSet
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
    public Iterator<String> getCurrentJobs() {
        return current.iterator();
    }

    @Override
    public boolean isFinished() {
        return pending.isEmpty();
    }

    @Override
    public boolean isFinished(String jobID) {
        //TODO: is Finished (specific job)
        return false;
    }

    @Override
    public ResultSet getResults() {
        //TODO: get Results (all)
        return null;
    }

    @Override
    public ResultSet getResults(String jobID) {
        //TODO: get Results (specific job)
        return null;
    }

    @Override
    public ResultSet exec() {
        //TODO: exec
        return null;
    }
}
