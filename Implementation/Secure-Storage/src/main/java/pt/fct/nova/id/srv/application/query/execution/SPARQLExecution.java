package pt.fct.nova.id.srv.application.query.execution;

import org.apache.jena.query.ResultSet;

import java.util.Iterator;

public interface SPARQLExecution {

    Iterator<String> getPendingJobs();

    Iterator<String> getFinishedJobs();

    Iterator<String> getCurrentJobs();

    boolean isFinished();

    boolean isFinished(String jobID);

    ResultSet getResults();

    ResultSet getResults(String jobID);

    ResultSet exec();
}
