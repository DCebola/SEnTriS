package pt.fct.nova.id.srv.application.query.jobs.jobs1;

import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;

import java.io.Serial;

public class BindJob extends BaseJob1 {
    @Serial
    private static final long serialVersionUID = 5545647838588746994L;
    private final Var variable;

    private final Expr expression;


    public BindJob(String jobID, String prevJobID, Var variable, Expr expression) {
        super(jobID, prevJobID);
        this.variable = variable;
        this.expression = expression;
    }

    public Var getVariable() {
        return variable;
    }

    public Expr getExpression() {
        return expression;
    }
}
