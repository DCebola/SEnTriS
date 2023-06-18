package pt.fct.nova.id.srv.application.query.execution;

import pt.fct.nova.id.srv.application.query.execution.exceptions.SPARQLExecutionException;

import java.util.Iterator;

public interface SPARQLExecution {

    Iterator<String> getPendingJobs();

    Iterator<String> getFinishedJobs();

    String getCurrentJob();

    boolean isFinished();

    boolean isFinished(String jobID);

    SPARQLResult<String> getResults();

    void exec(SPARQLWorker worker) throws SPARQLExecutionException;
}
