package pt.fct.nova.id.srv.application.query.plans;

import org.apache.jena.sparql.core.Var;
import pt.fct.nova.id.srv.application.query.jobs.Job;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

public class SimpleQueryExecutionPlan implements QueryExecutionPlan, Serializable {
    @Serial
    private static final long serialVersionUID = 6345655033367727690L;
    private final Map<String, Job> jobs;
    private final Deque<String> executionOrder;
    private final List<Var> vars;

    public SimpleQueryExecutionPlan() {
        this.jobs = new HashMap<>();
        this.executionOrder = new LinkedList<>();
        this.vars = new LinkedList<>();
    }

    @Override
    public Map<String, Job> getJobs() {
        return jobs;
    }

    @Override
    public Queue<String> getExecutionOrder() {
        return executionOrder;
    }

    @Override
    public void pushJob(Job job) {
        String jobID = job.getID();
        jobs.put(jobID, job);
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
