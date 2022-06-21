package pt.fct.nova.id.srv.application.query.plans;

import org.apache.jena.sparql.algebra.Op;

import java.util.List;

public interface SPARQLPlanner {
    QueryExecutionPlan generatePlan(Op op, List<String> resultVarNames);
}
