package pt.fct.nova.id.srv.application.query;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.Syntax;
import org.apache.jena.sparql.algebra.AlgebraGenerator;
import org.apache.jena.sparql.algebra.Op;

public class SPARQLQueryEngine implements QueryEngine {
    public final static String TYPE = "SPARQL";

    private final AlgebraGenerator algebraGenerator;
    private final Syntax syntax;

    public SPARQLQueryEngine(Syntax syntax) {
        this.algebraGenerator = new AlgebraGenerator();
        this.syntax = syntax;
    }

    @Override
    public QueryResult execQuery(String queryString) {
        Query query = QueryFactory.create(queryString, syntax);
        Op opActual = algebraGenerator.compile(query);
        return null;
    }
}
