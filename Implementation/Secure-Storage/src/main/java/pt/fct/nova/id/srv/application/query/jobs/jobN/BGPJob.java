package pt.fct.nova.id.srv.application.query.jobs.jobN;

import java.util.List;

public class BGPJob extends BaseJobN {
    public BGPJob(String jobID, List<String> previousJobIDs) {
        super(jobID, previousJobIDs);
    }
}
