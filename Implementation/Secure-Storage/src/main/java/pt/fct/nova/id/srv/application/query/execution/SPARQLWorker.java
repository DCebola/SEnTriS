package pt.fct.nova.id.srv.application.query.execution;

import org.apache.jena.sparql.engine.binding.Binding;
import pt.fct.nova.id.srv.application.query.jobs.Job;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.Job1;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.Job2;

public interface SPARQLWorker {

    Binding exec(Job job);

    Binding exec(Job1 job, Binding prevJobBindings);

    Binding exec(Job2 job, Binding left, Binding right);

}
