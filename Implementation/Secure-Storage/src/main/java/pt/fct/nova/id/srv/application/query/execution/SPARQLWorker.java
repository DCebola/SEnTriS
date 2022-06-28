package pt.fct.nova.id.srv.application.query.execution;

import org.apache.jena.sparql.engine.binding.Binding;
import pt.fct.nova.id.srv.application.query.jobs.Job;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.Job1;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.Job2;

import java.util.List;

public interface SPARQLWorker {

    List<Binding> exec(Job job);

    List<Binding> exec(Job1 job, List<Binding> prevJobBindings);

    List<Binding> exec(Job2 job, List<Binding> left, List<Binding> right);

}
