package pt.fct.nova.id.srv.application.query.jobs.jobN;

import pt.fct.nova.id.srv.application.query.jobs.BaseJob;

import java.util.List;

public class BaseJobN extends BaseJob implements JobN{

    private final List<String> prevJobIDs;

    public BaseJobN(String jobID, List<String> previousJobIDs) {
        super(jobID);
        this.prevJobIDs = previousJobIDs;
    }

    @Override
    public List<String> getPreviousJobIDs() {
        return prevJobIDs;
    }
}
