package pt.fct.nova.id.srv.application.query.execution;


import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import pt.fct.nova.id.srv.application.query.jobs.Job;
import pt.fct.nova.id.srv.application.query.jobs.jobN.JobN;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.Job1;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.Job2;

import java.util.List;
import java.util.Map;

public interface SPARQLWorker {

    Map<Var, List<String>> exec(Job job);

    Map<Var, List<String>> exec(Job1 job, Map<Var, List<String>> prevJobBindings);

    Map<Var, List<String>> exec(Job2 job, Map<Var, List<String>> left, Map<Var, List<String>> right);

    Map<Var, List<String>> exec(JobN job, List<Map<Var, List<String>>> prevJobsBindings);

    List<Binding> generateBindings(Map<Var, List<String>> jobBindings);
}
