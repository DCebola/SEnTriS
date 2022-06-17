package pt.fct.nova.id.srv.application.query.jobs;

import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;

import java.util.Map;

public class BindJob extends Job {

    private final Var variable;

    private final Expr expression;

    private final String prevJobID;

    public BindJob(String jobID, String prevJobID, Var variable, Expr expression) {
        super(jobID);
        this.prevJobID = prevJobID;
        this.variable = variable;
        this.expression = expression;
    }


    public Var getVariable() {
        return variable;
    }

    public Expr getExpression() {
        return expression;
    }

    public String getPrevJobID() {
        return prevJobID;
    }
}
