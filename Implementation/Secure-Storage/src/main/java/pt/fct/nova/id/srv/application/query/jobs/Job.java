package pt.fct.nova.id.srv.application.query.jobs;

public class Job {
    private final String jobID;

    public Job(String jobID) {
        this.jobID = jobID;
    }

    public String getID() {
        return jobID;
    }
}
