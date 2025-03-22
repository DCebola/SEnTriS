package pt.fct.nova.id.srv.application.query.jobs;

import java.io.Serial;

public class BaseJob implements Job {
    @Serial
    private static final long serialVersionUID = 6345655234367727694L;

    private final String jobID;

    public BaseJob(String jobID) {
        this.jobID = jobID;
    }

    public String getID() {
        return jobID;
    }
}
