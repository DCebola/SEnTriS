package pt.fct.nova.id.srv.application.query.execution.exceptions;

import pt.fct.nova.id.srv.application.query.jobs.VariablesPattern;

public class GetJobPatternException extends SPARQLExecutionException {
    public GetJobPatternException(String jobInstance, String job, VariablesPattern variablesPattern) {
        super(String.format("[%s , %s] - Wrong variable pattern: %s", jobInstance, job, variablesPattern));
    }
}
