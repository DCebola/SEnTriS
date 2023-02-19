package pt.fct.nova.id.srv.application.query.execution;


import pt.fct.nova.id.srv.application.query.execution.exceptions.SPARQLExecutionException;
import pt.fct.nova.id.srv.application.query.jobs.Job;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.Job1;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.Job2;
import pt.fct.nova.id.srv.application.storage.tables.BindingsTableV1;

public interface SPARQLWorkerV1 {
    BindingsTableV1 exec(Job job) throws SPARQLExecutionException;

    BindingsTableV1 exec(Job1 job, BindingsTableV1 prevJobResults) throws SPARQLExecutionException;

    BindingsTableV1 exec(Job2 job, BindingsTableV1 left, BindingsTableV1 right) throws SPARQLExecutionException;

    SPARQLResult generateResults(BindingsTableV1 jobResults);

}
