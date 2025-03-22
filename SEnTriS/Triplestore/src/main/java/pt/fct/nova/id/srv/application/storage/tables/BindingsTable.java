package pt.fct.nova.id.srv.application.storage.tables;

import org.apache.jena.sparql.core.Var;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface BindingsTable {

    void add(String patternIdx, Var var, String binding);

    Set<Var> getVars();

    Map<String, Set<String>> getBindings(Var var);

    Map<String, String> getPatternIdxs(Var var);

    Map<Var, Map<String, Set<String>>> getBindings();

    Map<Var, Map<String, String>> getPatternIdxs();

    List<List<String>> getPatterns();

    void project(Collection<Var> vars);

    BindingsTable join(BindingsTable other);

    BindingsTable union(BindingsTable other);

    BindingsTable leftOuterJoin(BindingsTable other);

    BindingsTable minus(BindingsTable other);

}
