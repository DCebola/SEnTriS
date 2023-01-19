package pt.fct.nova.id.srv.application.querying;

import org.apache.jena.atlas.lib.NotImplemented;
import pt.fct.nova.id.srv.application.querying.plans.QueryExecutionPlan;

public interface QueryEngine {
    QueryExecutionPlan getQueryPlan(String query) throws NotImplemented;
}
