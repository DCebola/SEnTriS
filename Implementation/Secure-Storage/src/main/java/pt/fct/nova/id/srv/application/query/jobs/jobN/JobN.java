package pt.fct.nova.id.srv.application.query.jobs.jobN;

import pt.fct.nova.id.srv.application.query.jobs.Job;

import java.util.List;

public interface JobN extends Job {
    List<String> getPreviousJobIDs();
}
