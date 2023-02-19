package pt.fct.nova.id.srv.application.storage.tables;

import org.apache.jena.sparql.core.Var;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface BindingsTableV1 {

    void add(String patternIdx, Var var, String binding);

    Set<Var> getVars();

    Map<String, Set<String>> getBindings(Var var);

    Map<String, String> getPatternIdxs(Var var);

    Map<Var, Map<String, Set<String>>> getBindings();

    Map<Var, Map<String, String>> getPatternIdxs();

    List<List<String>> getPatterns();

    void project(Collection<Var> vars);

    BindingsTableV1 join(BindingsTableV1 other);

    BindingsTableV1 union(BindingsTableV1 other);

    BindingsTableV1 leftOuterJoin(BindingsTableV1 other);

    BindingsTableV1 minus(BindingsTableV1 other);

}
