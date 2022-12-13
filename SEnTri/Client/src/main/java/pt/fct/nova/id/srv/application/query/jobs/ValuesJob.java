package pt.fct.nova.id.srv.application.query.jobs;


import org.apache.jena.sparql.engine.binding.Binding;

import java.io.Serial;
import java.util.List;

public class ValuesJob extends BaseJob {
    @Serial
    private static final long serialVersionUID = 6345655033367727694L;

    private final List<Binding> values;

    public ValuesJob(String jobID, List<Binding> values) {
        super(jobID);
        this.values = values;
    }

    public List<Binding> getValues() {
        return values;
    }

}
