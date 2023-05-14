package pt.fct.nova.id.srv.application.query.jobs;

import org.apache.jena.sparql.core.Var;

import java.io.Serial;
import java.util.List;

public class SecureSearchJob  extends BaseJob {
    @Serial
    private static final long serialVersionUID = 6345655033367727692L;

    private final List<Var> vars;
    private final List<String> trapdoors;

    public SecureSearchJob(String jobID, List<Var> vars, List<String> trapdoors) {
        super(jobID);
        this.vars = vars;
        this.trapdoors = trapdoors;
    }

    public List<Var> getVars() {
        return vars;
    }

    public List<String> getTrapdoors() {
        return trapdoors;
    }
}
