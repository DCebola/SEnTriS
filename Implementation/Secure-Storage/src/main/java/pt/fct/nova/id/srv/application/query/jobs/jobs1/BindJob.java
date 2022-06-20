package pt.fct.nova.id.srv.application.query.jobs.jobs1;

import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;

public class BindJob extends Job1 {

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
