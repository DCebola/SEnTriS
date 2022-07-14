package pt.fct.nova.id.srv.application.storage.iri_tables;

import org.apache.jena.sparql.core.Var;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IRITable {

    void addIRI(String patternIdx, Var var, String iri);

    Set<Var> getVars();

    Map<String, Set<String>> getIRIs(Var var);

    Map<String, String> getPatternIdxs(Var var);

    Map<Var, Map<String, Set<String>>> getIRIs();

    Map<Var, Map<String, String>> getPatternIdxs();

    Set<List<String>> getPatterns();

    void project(Collection<Var> vars);

    IRITable join(IRITable other);

    IRITable union(IRITable right);

    IRITable leftOuterJoin(IRITable right);

    IRITable minus(IRITable right);

}
