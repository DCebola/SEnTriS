package pt.fct.nova.id.srv.application.query.execution;


import org.apache.jena.sparql.engine.binding.Binding;
import pt.fct.nova.id.srv.application.query.jobs.Job;
import pt.fct.nova.id.srv.application.query.jobs.jobN.JobN;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.Job1;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.Job2;
import pt.fct.nova.id.srv.application.storage.iri_tables.IRITable;

import java.util.List;

public interface SPARQLWorker {

    IRITable exec(Job job);

    IRITable exec(Job1 job, IRITable prevJobResults);

    IRITable exec(Job2 job, IRITable left, IRITable right);

    IRITable exec(JobN job, List<IRITable> prevJobsResults);

    List<Binding> generateBindings(IRITable jobResults);
}
