package pt.fct.nova.id.srv.application.query.execution;

import pt.fct.nova.id.srv.application.crypto.dgk.HomomorphicException;
import pt.fct.nova.id.srv.application.query.execution.exceptions.SPARQLExecutionException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

public interface SPARQLExecutionV2 {

    Iterator<String> getPendingJobs();

    Iterator<String> getFinishedJobs();

    String getCurrentJob();

    boolean isFinished();

    boolean isFinished(String jobID);

    SPARQLResult getResults();

    void exec(SPARQLWorkerV2 worker) throws SPARQLExecutionException;
}
