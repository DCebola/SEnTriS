package pt.fct.nova.id.srv.application.query.execution.exceptions;

public class JobInstanceException extends SPARQLExecutionException {
    public JobInstanceException(String jobInstance, String job) {
        super(String.format("[%s , %s] - Incorrect instance.", jobInstance, job));
    }
}
