package pt.fct.nova.id.srv.application.query.execution;


import org.apache.jena.sparql.engine.binding.Binding;
import pt.fct.nova.id.srv.application.query.jobs.Job;
import pt.fct.nova.id.srv.application.query.jobs.jobN.JobN;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.Job1;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.Job2;
import pt.fct.nova.id.srv.application.storage.idx_data_structs.IdxTable;

import java.util.List;

public interface SPARQLWorker {

    IdxTable exec(Job job);

    IdxTable exec(Job1 job, IdxTable prevJobResults);

    IdxTable exec(Job2 job, IdxTable left, IdxTable right);

    IdxTable exec(JobN job, List<IdxTable> prevJobsResults);

    List<Binding> generateBindings(IdxTable jobResults);
}
