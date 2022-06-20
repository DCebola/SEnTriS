package pt.fct.nova.id.srv.application.query.jobs.jobs2;

import pt.fct.nova.id.srv.application.query.jobs.Job;

public class Job2 extends Job {
    private final String leftJobID;

    private final String rightJobID;

    public Job2(String jobID, String leftJobID, String rightJobID) {
        super(jobID);
        this.leftJobID = leftJobID;
        this.rightJobID = rightJobID;
    }

    public String getLeftJobID() {
        return leftJobID;
    }

    public String getRightJobID() {
        return rightJobID;
    }
}
