package pt.fct.nova.id.srv.application.query;

import pt.fct.nova.id.srv.application.query.plans.QueryExecutionPlan;
import pt.fct.nova.id.srv.application.storage.StorageEngine;

public interface QueryEngine {

    QueryExecutionPlan getQueryPlan(String query);
}
