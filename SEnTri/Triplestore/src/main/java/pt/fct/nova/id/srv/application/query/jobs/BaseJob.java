package pt.fct.nova.id.srv.application.query.jobs;

public class BaseJob implements Job {
    private final String jobID;

    public BaseJob(String jobID) {
        this.jobID = jobID;
    }

    public String getID() {
        return jobID;
    }
}
