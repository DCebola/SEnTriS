package pt.fct.nova.id.srv.application.query.execution;

import org.apache.jena.query.ResultSet;
import pt.fct.nova.id.srv.application.storage.StorageEngine;

import java.util.Iterator;

public interface SPARQLExecution {

    Iterator<String> getPendingJobs();

    Iterator<String> getFinishedJobs();

    String getCurrentJob();

    boolean isFinished();

    boolean isFinished(String jobID);

    ResultSet getResults();

    ResultSet exec(String storeID, StorageEngine storageEngine);
}
