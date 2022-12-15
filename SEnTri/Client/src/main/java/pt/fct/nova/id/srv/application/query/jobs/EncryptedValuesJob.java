package pt.fct.nova.id.srv.application.query.jobs;

import java.io.Serial;
import java.util.List;

public class EncryptedValuesJob extends BaseJob {
    @Serial
    private static final long serialVersionUID = 3345654478562467694L;

    private final List<EncryptedBinding> values;

    public EncryptedValuesJob(String jobID, List<EncryptedBinding> values) {
        super(jobID);
        this.values = values;

    }

    public List<EncryptedBinding> getValues() {
        return values;
    }

}
