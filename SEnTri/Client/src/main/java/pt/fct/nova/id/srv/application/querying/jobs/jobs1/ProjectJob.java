package pt.fct.nova.id.srv.application.querying.jobs.jobs1;

import org.apache.jena.sparql.core.Var;

import java.io.Serial;
import java.util.List;

public class ProjectJob extends BaseJob1 {
    @Serial
    private static final long serialVersionUID = 5545662238392523294L;

    private final List<Var> variables;

    public ProjectJob(String jobID, String prevJobID, List<Var> variables) {
        super(jobID, prevJobID);
        this.variables = variables;
    }

    public List<Var> getVars() {
        return variables;
    }

}
