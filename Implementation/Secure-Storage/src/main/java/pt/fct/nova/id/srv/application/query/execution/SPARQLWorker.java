package pt.fct.nova.id.srv.application.query.execution;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import pt.fct.nova.id.srv.application.query.jobs.Job;
import pt.fct.nova.id.srv.application.query.jobs.jobN.JobN;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.Job1;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.Job2;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface SPARQLWorker {

    Map<Var, Set<String>> exec(Job job);

    Map<Var, Set<String>> exec(Job1 job, Map<Var, Set<String>> prevJobBindings);

    Map<Var, Set<String>> exec(Job2 job, Map<Var, Set<String>> left, Map<Var, Set<String>> right);

    Map<Var, Set<String>> exec(JobN job, List<Map<Var, Set<String>>> prevJobsBindings);

    List<Binding> generateBindings(Map<Var, Set<String>> jobBindings);
}
