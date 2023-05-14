package pt.fct.nova.id.srv.application.query.execution;


import pt.fct.nova.id.srv.application.query.execution.exceptions.SPARQLExecutionException;
import pt.fct.nova.id.srv.application.query.jobs.Job;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.Job1;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.Job2;
import pt.fct.nova.id.srv.application.storage.tables.BindingsTable;

public interface SPARQLWorker {
    BindingsTable exec(Job job) throws SPARQLExecutionException;

    BindingsTable exec(Job1 job, BindingsTable prevJobResults) throws SPARQLExecutionException;

    BindingsTable exec(Job2 job, BindingsTable left, BindingsTable right) throws SPARQLExecutionException;

    SPARQLResult generateResults(BindingsTable jobResults) throws SPARQLExecutionException;
}
