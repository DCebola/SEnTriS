package pt.fct.nova.id.srv.application.query;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.Syntax;
import org.apache.jena.sparql.algebra.*;
import org.apache.jena.sparql.algebra.walker.WalkerVisitor;

public class SPARQLQueryEngine implements QueryEngine {
    public final static String TYPE = "SPARQL";

    private final AlgebraGenerator algebraGenerator;

    public SPARQLQueryEngine() {
        this.algebraGenerator = new AlgebraGenerator();
    }

    @Override
    public QueryResult execQuery(String queryString) {
        Query query = QueryFactory.create(queryString, Syntax.syntaxSPARQL_11);

        Op opActual = algebraGenerator.compile(query);
        opActual.visit(new WalkerVisitor());
        return null;
    }
}
