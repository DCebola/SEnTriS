package pt.fct.nova.id.srv.application;

import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.query.*;
import org.apache.jena.sparql.algebra.AlgebraGenerator;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import pt.fct.nova.id.srv.application.query.QueryUtils;
import pt.fct.nova.id.srv.application.query.plans.*;

import static pt.fct.nova.id.srv.application.query.QueryType.CONSTRUCT;


public class SPARQLQueryEngine implements QueryEngine {

    private final AlgebraGenerator algebraGenerator;
    private final SPARQLPlanner planner;

    public SPARQLQueryEngine(SPARQLPlanner planner) {
        this.algebraGenerator = new AlgebraGenerator(ARQ.getContext());
        this.planner = planner;
    }

    public QueryExecutionPlan getQueryPlan(String queryString) throws NotImplemented {
        try {
            Query query = QueryFactory.create(queryString);
            planner.setQueryType(QueryUtils.convertQueryType(query.queryType()));
            if (planner.getQueryType() == CONSTRUCT)
                planner.setConstructTemplate(query.getConstructTemplate().getTriples());
            return planner.generatePlan(algebraGenerator.compile(query));
        } catch (QueryParseException e){
            System.out.println(e.getMessage());
            UpdateRequest update = UpdateFactory.create(queryString);
            return planner.generatePlan(update.getOperations().get(0), algebraGenerator);
        }
    }
}
