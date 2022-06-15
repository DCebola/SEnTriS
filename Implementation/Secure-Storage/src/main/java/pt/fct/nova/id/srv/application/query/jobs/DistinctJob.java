package pt.fct.nova.id.srv.application.query.jobs;

import pt.fct.nova.id.srv.application.query.jobs.Job;

public class DistinctJob extends Job {

    private final String prevJobID;

    public DistinctJob(String jobID, String prevJobID) {
        super(jobID);
        this.prevJobID = prevJobID;
    }

    public String getPrevJobID() {
        return prevJobID;
    }
}
