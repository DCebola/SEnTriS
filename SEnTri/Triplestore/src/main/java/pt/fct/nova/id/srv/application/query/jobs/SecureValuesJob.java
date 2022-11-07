package pt.fct.nova.id.srv.application.query.jobs;

import pt.fct.nova.id.srv.application.storage.iri_tables.IRITable;

public class SecureValuesJob extends BaseJob{

    private final IRITable values;
    public SecureValuesJob(String jobID, IRITable values) {
        super(jobID);
        this.values = values;
    }

    public IRITable getValues() {
        return values;
    }
}
