package pt.fct.nova.id.srv.application.query.plans;

import org.apache.jena.sparql.core.Var;
import pt.fct.nova.id.srv.application.query.jobs.Job;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public interface QueryExecutionPlan extends Serializable {

    Map<String, Job> getJobs();

    Queue<String> getExecutionOrder();

    void pushJob(Job job);

    List<Var> getVars();

    void setVars(List<Var> vars);
}
