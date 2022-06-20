package pt.fct.nova.id.srv.application.query.plans;

import org.apache.jena.sparql.algebra.Op;

public interface SPARQLPlanner {
    ExecutionPlan generatePlan(Op op);
}
