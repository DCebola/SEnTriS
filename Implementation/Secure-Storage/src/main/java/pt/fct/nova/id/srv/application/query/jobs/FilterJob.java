package pt.fct.nova.id.srv.application.query.jobs;

import org.apache.jena.sparql.expr.Expr;
import pt.fct.nova.id.srv.application.query.jobs.Job;

import java.util.List;

public class FilterJob extends Job {

    private final String prevJobID;

    private final List<Expr> expressions;

    public FilterJob(String jobID, String prevJobID, List<Expr> expressions) {
        super(jobID);
        this.prevJobID = prevJobID;
        this.expressions = expressions;
    }

    public String getPrevJobID() {
        return prevJobID;
    }

    public List<Expr> getExpressions() {
        return expressions;
    }
}
