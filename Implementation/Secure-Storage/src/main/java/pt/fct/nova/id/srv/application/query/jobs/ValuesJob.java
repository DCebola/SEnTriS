package pt.fct.nova.id.srv.application.query.jobs;


import org.apache.jena.sparql.engine.binding.Binding;

import java.util.Iterator;
import java.util.Set;

public class ValuesJob extends Job {

    private final Set<Binding> values;

    public ValuesJob(String jobID, Set<Binding> values) {
        super(jobID);
        this.values = values;
    }

    public Set<Binding> getValues() {
        return values;
    }

}
