package pt.fct.nova.id.srv.application.query.jobs;

import org.apache.jena.sparql.core.Var;

import java.util.Set;

public class EmptyResJob extends BaseJob{

    private final Set<Var> vars;

    public EmptyResJob(String jobID, Set<Var> vars) {
        super(jobID);
        this.vars = vars;
    }

    public Set<Var> getVars() {
        return vars;
    }
}
