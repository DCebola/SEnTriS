package pt.fct.nova.id.srv.application.query.execution;

import pt.fct.nova.id.srv.application.query.execution.exceptions.SPARQLExecutionException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

public interface SPARQLExecution {

    Iterator<String> getPendingJobs();

    Iterator<String> getFinishedJobs();

    String getCurrentJob();

    boolean isFinished();

    boolean isFinished(String jobID);

    SPARQLResult getResults();

    void exec(SPARQLWorker worker) throws SPARQLExecutionException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException;
}
