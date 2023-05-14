package pt.fct.nova.id.srv.application.query.jobs.jobs2;

import pt.fct.nova.id.srv.application.query.jobs.Job;

public interface Job2 extends Job {

    String getLeftJobID();

    String getRightJobID();
}
