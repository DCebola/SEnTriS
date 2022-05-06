package pt.fct.nova.id.srv.application.query;

public class QueryEngineFactory {
    public static QueryEngine createNewQueryEngine(String type) {
        return new SPARQLQueryEngine();
    }
}
