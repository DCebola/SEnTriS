package pt.fct.nova.id.srv.application;

import org.apache.jena.atlas.lib.NotImplemented;
import pt.fct.nova.id.srv.application.query.execution.exceptions.SPARQLExecutionException;
import pt.fct.nova.id.srv.application.query.plans.QueryExecutionPlan;

public interface QueryEngine {
    QueryExecutionPlan getQueryPlan(String query) throws NotImplemented, SPARQLExecutionException;
}
