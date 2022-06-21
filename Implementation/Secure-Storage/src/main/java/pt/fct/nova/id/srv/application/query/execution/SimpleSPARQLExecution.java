package pt.fct.nova.id.srv.application.query.execution;

import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ResultSetStream;
import org.apache.jena.sparql.engine.binding.Binding;
import pt.fct.nova.id.srv.application.query.jobs.Job;
import pt.fct.nova.id.srv.application.query.plans.ExecutionPlan;

import java.util.*;

public class SimpleSPARQLExecution implements SPARQLExecution {

    private final Map<Job, String> jobs;
    private final List<String> current;
    private final Queue<String> pending;
    private final List<String> finished;
    private final Map<String, Binding> jobBindings;
    private final List<Var> vars;


    public SimpleSPARQLExecution(ExecutionPlan plan) {
        this.vars = plan.getVars();
        this.jobs = plan.getJobs();
        this.pending = plan.getExecutionOrder();
        this.finished = new LinkedList<>();
        this.current = new LinkedList<>();
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
    public Iterator<String> getCurrentJobs() {
        return current.iterator();
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
        return ResultSetStream.create(vars, jobBindings.values().iterator());
    }

    @Override
    public ResultSet getResults(String jobID) {
        List<Binding> binding = new ArrayList<>(1);
        binding.add(jobBindings.get(jobID));
        return ResultSetStream.create(vars, binding.iterator());
    }

    @Override
    public ResultSet exec() {
        //TODO: exec
        return null;
    }
}
