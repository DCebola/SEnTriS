package pt.fct.nova.id.srv.application.query.execution.exceptions;

public class SecureSearchException extends SPARQLExecutionException{
    public SecureSearchException(String jobInstance, String job, int numVars) {
        super(String.format("[%s , %s] - Number of variables must be 1 or 2: %s", jobInstance, job, numVars));
    }
}
