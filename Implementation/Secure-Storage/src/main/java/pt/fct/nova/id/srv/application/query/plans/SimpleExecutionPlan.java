package pt.fct.nova.id.srv.application.query.plans;

import org.apache.jena.sparql.core.Var;
import pt.fct.nova.id.srv.application.query.jobs.Job;

import java.util.*;

public class SimpleExecutionPlan implements ExecutionPlan {

    private final Map<Job, String> jobs;
    private final Deque<String> executionOrder;
    private final List<Var> vars;

    public SimpleExecutionPlan() {
        this.jobs = new HashMap<>();
        this.executionOrder = new LinkedList<>();
        this.vars = new LinkedList<>();
    }


    @Override
    public Map<Job, String> getJobs() {
        return jobs;
    }

    @Override
    public Queue<String> getExecutionOrder() {
        return executionOrder;
    }

    @Override
    public void pushJob(Job job) {
        String jobID = job.getID();
        jobs.put(job, jobID);
        executionOrder.addLast(jobID);
    }

    @Override
    public List<Var> getVars() {
        return vars;
    }

    @Override
    public void setVars(List<Var> vars) {
        if (!this.vars.isEmpty())
            this.vars.clear();
        this.vars.addAll(vars);
    }
}
