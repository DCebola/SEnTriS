package pt.fct.nova.id.srv.application.query.jobs;

public class Job {
    private final VariablesPattern varsPattern;
    private final String jobID;

    public Job(VariablesPattern varsPattern, String jobID) {
        this.varsPattern = varsPattern;
        this.jobID = jobID;
    }

    public VariablesPattern getVariablesPattern() {
        return varsPattern;
    }

    public String getID() {
        return jobID;
    }
}
