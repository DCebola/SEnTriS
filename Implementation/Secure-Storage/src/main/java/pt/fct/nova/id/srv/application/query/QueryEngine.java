package pt.fct.nova.id.srv.application.query;

import org.apache.jena.query.ResultSet;

public interface QueryEngine {

    ResultSet execQuery(String query);
}
