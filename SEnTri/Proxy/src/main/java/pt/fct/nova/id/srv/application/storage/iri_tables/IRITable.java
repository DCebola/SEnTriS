package pt.fct.nova.id.srv.application.storage.iri_tables;

import org.apache.jena.sparql.core.Var;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IRITable {

    void add(Map<Var, String> binding);

    Set<Var> getVars();

    Map<String, Set<Integer>> getIRIs(Var var);

    Map<Integer, String> getPatternIdxs(Var var);

    Map<Var, Map<String, Set<Integer>>> getIRIs();

    Map<Var, Map<Integer, String>> getPatternIdxs();

    List<List<String>> getPatterns();

    void project(Collection<Var> vars);

    IRITable join(IRITable other);

    IRITable union(IRITable other);

    IRITable leftOuterJoin(IRITable other);

    IRITable minus(IRITable other);

}
