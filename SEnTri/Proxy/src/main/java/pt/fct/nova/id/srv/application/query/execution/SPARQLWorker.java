package pt.fct.nova.id.srv.application.query.execution;


import org.apache.jena.sparql.engine.binding.Binding;
import pt.fct.nova.id.srv.application.query.execution.exceptions.GetJobPatternException;
import pt.fct.nova.id.srv.application.query.execution.exceptions.JobInstanceException;
import pt.fct.nova.id.srv.application.query.execution.exceptions.SPARQLExecutionException;
import pt.fct.nova.id.srv.application.query.jobs.Job;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.Job1;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.Job2;
import pt.fct.nova.id.srv.application.storage.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.storage.iri_tables.IRITable;

import java.util.Collection;
import java.util.List;

public interface SPARQLWorker {
    IRITable exec(Job job) throws SPARQLExecutionException;

    IRITable exec(Job1 job, IRITable prevJobResults) throws SPARQLExecutionException;

    IRITable exec(Job2 job, IRITable left, IRITable right) throws SPARQLExecutionException;

    Collection<Binding> generateBindings(IRITable jobResults) throws SPARQLExecutionException;
}
