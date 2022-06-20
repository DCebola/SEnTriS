package pt.fct.nova.id.srv.application.query.jobs.jobs1;

import org.apache.jena.sparql.core.Var;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.Job1;

import java.util.List;

public class ProjectJob extends Job1 {

    private final List<Var> variables;

    public ProjectJob(String jobID, String prevJobID, List<Var> variables) {
        super(jobID, prevJobID);
        this.variables = variables;
    }

    public List<Var> getVariables() {
        return variables;
    }

}
