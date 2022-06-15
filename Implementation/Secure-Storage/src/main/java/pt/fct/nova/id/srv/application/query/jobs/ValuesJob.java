package pt.fct.nova.id.srv.application.query.jobs;


import org.apache.jena.sparql.engine.binding.Binding;
import java.util.List;

public class ValuesJob extends Job {

    private final List<Binding> values;

    public ValuesJob(String jobID, List<Binding> values) {
        super(jobID);
        this.values = values;
    }

    public List<Binding> getValues() {
        return values;
    }

}
