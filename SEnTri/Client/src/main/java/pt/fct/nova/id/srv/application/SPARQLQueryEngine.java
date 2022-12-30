package pt.fct.nova.id.srv.application;

import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.query.ARQ;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryType;
import org.apache.jena.sparql.algebra.AlgebraGenerator;
import pt.fct.nova.id.srv.application.query.plans.*;

import java.util.regex.Pattern;

import static org.apache.jena.query.QueryType.*;

public class SPARQLQueryEngine implements QueryEngine {

    private final AlgebraGenerator algebraGenerator;
    private final SPARQLPlanner planner;

    public SPARQLQueryEngine(SPARQLPlanner planner) {
        this.algebraGenerator = new AlgebraGenerator(ARQ.getContext());
        this.planner = planner;
    }

    public QueryExecutionPlan getQueryPlan(String queryString) throws NotImplemented {
        Query query = QueryFactory.create(queryString);
       planner.setQueryType(query.queryType());
        if (planner.getQueryType() == CONSTRUCT)
            planner.setConstructTemplate(query.getConstructTemplate().getTriples());
        return planner.generatePlan(algebraGenerator.compile(query), query.getResultVars());
    }
}
