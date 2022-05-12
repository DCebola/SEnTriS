package pt.fct.nova.id.srv.application.query;

import pt.fct.nova.id.srv.application.query.bindings.BindingRow;

import java.util.List;
import java.util.Set;

public interface QueryResult {

    Set<String> getLinks();

    Set<String> getVariables();

    List<BindingRow> getBindings();

    boolean getBoolean();

}
