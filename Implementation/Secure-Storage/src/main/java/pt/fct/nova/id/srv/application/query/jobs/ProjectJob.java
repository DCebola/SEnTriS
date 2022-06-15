package pt.fct.nova.id.srv.application.query.jobs;

import org.apache.jena.sparql.core.Var;

import java.util.List;

public class ProjectJob extends Job {

    private final List<Var> variables;

    private final String prevJobID;

    public ProjectJob(String jobID, String prevJobID, List<Var> variables) {
        super(jobID);
        this.variables = variables;
        this.prevJobID = prevJobID;
    }

    public List<Var> getVariables() {
        return variables;
    }

    public String getPrevJobID() {
        return prevJobID;
    }
}
