package pt.fct.nova.id.srv.application.query.execution.exceptions;

public class JobNotImplementedException extends SPARQLExecutionException {
    public JobNotImplementedException(String jobInstance, String job) {
        super(String.format("[%s , %s] - Not implemented.", jobInstance, job));
    }
}
