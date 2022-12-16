package pt.fct.nova.id.srv.application.query.jobs;

import org.apache.jena.sparql.core.Var;

import java.io.Serial;
import java.util.Set;

public class SecureSearchJob  extends BaseJob {
    @Serial
    private static final long serialVersionUID = 6345655033367727692L;

    private final Set<Var> vars;

    public SecureSearchJob(String jobID, Set<Var> vars) {
        super(jobID);
        this.vars = vars;
    }

    public Set<Var> getVars() {
        return vars;
    }

}
