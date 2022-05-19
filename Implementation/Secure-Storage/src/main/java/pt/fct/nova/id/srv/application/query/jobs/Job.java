package pt.fct.nova.id.srv.application.query.jobs;

public class Job {
    private final VariablesPattern varsPattern;

    public Job(VariablesPattern varsPattern) {
        this.varsPattern = varsPattern;

    }

    public VariablesPattern getVariablesPattern() {
        return varsPattern;
    }

}
