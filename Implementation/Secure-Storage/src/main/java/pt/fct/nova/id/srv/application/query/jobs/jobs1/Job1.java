package pt.fct.nova.id.srv.application.query.jobs.jobs1;

import pt.fct.nova.id.srv.application.query.jobs.Job;

public class Job1 extends Job {

    private final String prevJobID;

    public Job1(String jobID, String prevJobID) {
        super(jobID);
        this.prevJobID = prevJobID;
    }

    public String getPrevJobID() {
        return prevJobID;
    }
}
