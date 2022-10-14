package pt.fct.nova.id.srv.application.query;

import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.query.*;
import org.apache.jena.sparql.algebra.*;
import pt.fct.nova.id.srv.application.query.plans.QueryExecutionPlan;
import pt.fct.nova.id.srv.application.query.plans.SPARQLPlanner;
import pt.fct.nova.id.srv.application.query.plans.SimpleSPARQLPlanner;

public class SPARQLQueryEngine implements QueryEngine {

    private final AlgebraGenerator algebraGenerator;
    private final SPARQLPlanner planner;

    public SPARQLQueryEngine() {
        this.algebraGenerator = new AlgebraGenerator(ARQ.getContext());
        this.planner = new SimpleSPARQLPlanner();
    }

    public QueryExecutionPlan getQueryPlan(String queryString) throws NotImplemented {
        Query query = QueryFactory.create(queryString);
        if (query.queryType().equals(QueryType.SELECT))
            return planner.generatePlan(algebraGenerator.compile(query), query.getResultVars());
        else throw new NotImplemented();
    }
}
