package pt.fct.nova.id.srv.application.query.jobs.jobs1;

import pt.fct.nova.id.srv.application.query.jobs.BaseJob;

import java.io.Serial;

public class BaseJob1 extends BaseJob implements Job1 {
    @Serial
    private static final long serialVersionUID = 5545635528588746994L;
    private final String prevJobID;

    public BaseJob1(String jobID, String prevJobID) {
        super(jobID);
        this.prevJobID = prevJobID;
    }

    public String getPrevJobID() {
        return prevJobID;
    }
}
