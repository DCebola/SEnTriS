package pt.fct.nova.id.srv.application.query.plans;

import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.QueryType;
import org.apache.jena.sparql.algebra.Op;

import java.util.List;

public interface SPARQLPlanner {
    QueryExecutionPlan generatePlan(Op op, List<String> resultVars) throws NotImplemented;
    void setQueryType(QueryType queryType);
    QueryType getQueryType();

    void setConstructTemplate(List<Triple> constructTemplate);

    List<Triple> getConstructTemplate();

}
