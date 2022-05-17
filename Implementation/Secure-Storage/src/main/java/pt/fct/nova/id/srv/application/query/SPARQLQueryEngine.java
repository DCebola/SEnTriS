package pt.fct.nova.id.srv.application.query;

import org.apache.jena.query.*;
import org.apache.jena.sparql.algebra.*;
import org.apache.jena.sparql.engine.iterator.QueryIter;
import org.apache.jena.sparql.engine.iterator.QueryIterPlainWrapper;

public class SPARQLQueryEngine implements QueryEngine {
    public final static String TYPE = "SPARQL";
    private final AlgebraGenerator algebraGenerator;

    public SPARQLQueryEngine() {
        this.algebraGenerator = new AlgebraGenerator();
    }

    @Override
    public ResultSet execQuery(String queryString) {
        Query query = QueryFactory.create(queryString, Syntax.syntaxSPARQL_11);
        SPARQLVisitor visitor = new SPARQLVisitor();
        algebraGenerator.compile(query).visit(visitor);

        return null;
    }
}
