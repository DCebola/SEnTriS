package pt.fct.nova.id.srv.application.query.jobs.jobs1;

import org.apache.jena.sparql.expr.Expr;

import java.util.List;

public class FilterJob extends Job1 {

    private final List<Expr> expressions;

    public FilterJob(String jobID, String prevJobID, List<Expr> expressions) {
        super(jobID, prevJobID);
        this.expressions = expressions;
    }


    public List<Expr> getExpressions() {
        return expressions;
    }
}
