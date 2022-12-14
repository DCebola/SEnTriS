package pt.fct.nova.id.srv.application.query.jobs;


import java.io.Serial;
import java.util.List;

public class ValuesJob extends BaseJob {
    @Serial
    private static final long serialVersionUID = 6345655033367727694L;

    private final List<SerializableBinding> values;

    public ValuesJob(String jobID, List<SerializableBinding> values) {
        super(jobID);
        this.values = values;

    }

    public  List<SerializableBinding>  getValues() {
        return values;
    }

}
