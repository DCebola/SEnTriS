package pt.fct.nova.id.srv.application.query.jobs.jobs2;

import pt.fct.nova.id.srv.application.query.jobs.BaseJob;

import java.io.Serial;

public class BaseJob2 extends BaseJob implements Job2 {
    @Serial
    private static final long serialVersionUID = 6345662348367727694L;
    private final String leftJobID;

    private final String rightJobID;

    public BaseJob2(String jobID, String leftJobID, String rightJobID) {
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
