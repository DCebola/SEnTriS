package pt.fct.nova.id.srv.application.query;

import org.apache.jena.query.*;
import org.apache.jena.sparql.algebra.*;

public class SPARQLQueryEngine implements QueryEngine {
    private final AlgebraGenerator algebraGenerator;

    public SPARQLQueryEngine() {
        this.algebraGenerator = new AlgebraGenerator();
    }

    @Override
    public ResultSet execQuery(String queryString) {
        Query query = QueryFactory.create(queryString);

        SPARQLPlanner visitor = new SPARQLPlanner();
        visitor.opVisitorWalker(algebraGenerator.compile(query));
        return null;
    }
}
