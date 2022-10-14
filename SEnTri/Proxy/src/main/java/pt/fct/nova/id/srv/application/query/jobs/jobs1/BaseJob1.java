package pt.fct.nova.id.srv.application.query.jobs.jobs1;

import pt.fct.nova.id.srv.application.query.jobs.BaseJob;

public class BaseJob1 extends BaseJob implements Job1 {

    private final String prevJobID;

    public BaseJob1(String jobID, String prevJobID) {
        super(jobID);
        this.prevJobID = prevJobID;
    }

    public String getPrevJobID() {
        return prevJobID;
    }
}
