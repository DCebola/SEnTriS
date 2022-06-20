package pt.fct.nova.id.srv.application.query.plans;

import pt.fct.nova.id.srv.application.query.jobs.Job;

import java.util.*;

public class SimpleExecutionPlan implements ExecutionPlan {

    private final Map<Job, String> jobs;
    private final Deque<String> executionOrder;

    public SimpleExecutionPlan() {
        this.jobs = new HashMap<>();
        this.executionOrder = new LinkedList<>();
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
}
