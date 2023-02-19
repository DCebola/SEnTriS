package pt.fct.nova.id.srv.application.query.execution;


import pt.fct.nova.id.srv.application.query.execution.exceptions.SPARQLExecutionException;
import pt.fct.nova.id.srv.application.query.jobs.Job;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.Job1;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.Job2;
import pt.fct.nova.id.srv.application.storage.tables.BindingsTableV2;

public interface SPARQLWorkerV2 {
    BindingsTableV2 exec(Job job) throws SPARQLExecutionException;

    BindingsTableV2 exec(Job1 job, BindingsTableV2 prevJobResults) throws SPARQLExecutionException;

    BindingsTableV2 exec(Job2 job, BindingsTableV2 left, BindingsTableV2 right) throws SPARQLExecutionException;

    SPARQLResult generateResults(BindingsTableV2 jobResults);

}
