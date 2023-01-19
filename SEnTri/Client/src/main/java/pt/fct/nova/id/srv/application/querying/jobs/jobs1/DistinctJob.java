package pt.fct.nova.id.srv.application.querying.jobs.jobs1;

import java.io.Serial;

public class DistinctJob extends BaseJob1 {
    @Serial
    private static final long serialVersionUID = 5545647838589763294L;
    public DistinctJob(String jobID, String prevJobID) {
        super(jobID, prevJobID);
    }
}
