package pt.fct.nova.id.srv.application.querying.jobs.jobs2;

import java.io.Serial;

public class JoinJob extends BaseJob2 {
    @Serial
    private static final long serialVersionUID = 6345662348365527694L;
    public JoinJob(String jobID, String leftJobID, String rightJobID) {
        super(jobID, leftJobID, rightJobID);
    }
}
