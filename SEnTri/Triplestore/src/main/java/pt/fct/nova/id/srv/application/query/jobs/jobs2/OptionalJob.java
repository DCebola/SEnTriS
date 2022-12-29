package pt.fct.nova.id.srv.application.query.jobs.jobs2;

import java.io.Serial;

public class OptionalJob extends BaseJob2 {
    @Serial
    private static final long serialVersionUID = 6345662348392524494L;
    public OptionalJob(String jobID, String leftJobID, String rightJobID) {
        super(jobID, leftJobID, rightJobID);
    }
}
