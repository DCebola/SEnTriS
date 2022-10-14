package pt.fct.nova.id.srv.application.query.execution.exceptions;

public class SPARQLExecutionException extends RuntimeException{
    public SPARQLExecutionException(String errorMessage) {
        super(errorMessage);
    }
}
