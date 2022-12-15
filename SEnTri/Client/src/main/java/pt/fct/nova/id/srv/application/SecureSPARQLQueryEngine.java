package pt.fct.nova.id.srv.application;

import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryType;
import org.apache.jena.sparql.core.Var;
import pt.fct.nova.id.srv.application.query.plans.QueryInlineBindingsExtractor;
import pt.fct.nova.id.srv.application.query.plans.SPARQLPlanner;

import java.util.Map;

public class SecureSPARQLQueryEngine extends SPARQLQueryEngine{
    private final QueryInlineBindingsExtractor trapdoorExtractor;

    public SecureSPARQLQueryEngine(SPARQLPlanner planner) {
        super(planner);
        trapdoorExtractor = new QueryInlineBindingsExtractor();
    }

    public int getTotalBindings(String queryString) throws NotImplemented {
        Query query = QueryFactory.create(queryString);
        if (query.queryType().equals(QueryType.SELECT))
            return trapdoorExtractor.parse(super.getAlgebraGenerator().compile(query));
        else throw new NotImplemented();
    }
}
