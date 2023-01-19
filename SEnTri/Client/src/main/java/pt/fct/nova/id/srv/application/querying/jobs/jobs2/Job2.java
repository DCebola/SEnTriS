package pt.fct.nova.id.srv.application.querying.jobs.jobs2;

import pt.fct.nova.id.srv.application.querying.jobs.Job;

public interface Job2 extends Job {

    String getLeftJobID();

    String getRightJobID();
}
