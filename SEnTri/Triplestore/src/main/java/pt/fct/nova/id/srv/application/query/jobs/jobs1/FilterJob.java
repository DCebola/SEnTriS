package pt.fct.nova.id.srv.application.query.jobs.jobs1;

import org.apache.jena.sparql.expr.Expr;

import java.io.Serial;
import java.util.List;

public class FilterJob extends BaseJob1 {
    @Serial
    private static final long serialVersionUID = 5545662238589763294L;
    private final List<Expr> expressions;

    public FilterJob(String jobID, String prevJobID, List<Expr> expressions) {
        super(jobID, prevJobID);
        this.expressions = expressions;
    }


    public List<Expr> getExpressions() {
        return expressions;
    }
}
