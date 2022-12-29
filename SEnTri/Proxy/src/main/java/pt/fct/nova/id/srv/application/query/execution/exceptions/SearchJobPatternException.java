package pt.fct.nova.id.srv.application.query.execution.exceptions;

import pt.fct.nova.id.srv.application.query.jobs.VariablesPattern;

public class SearchJobPatternException extends SPARQLExecutionException {
    public SearchJobPatternException(String jobInstance, String job, VariablesPattern variablesPattern) {
        super(String.format("[%s , %s] - Wrong variable pattern: %s", jobInstance, job, variablesPattern));
    }
}
