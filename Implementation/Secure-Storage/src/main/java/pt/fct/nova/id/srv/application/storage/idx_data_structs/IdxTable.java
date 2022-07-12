package pt.fct.nova.id.srv.application.storage.idx_data_structs;

import org.apache.jena.sparql.core.Var;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IdxTable {

    void addIdx(String tripleIdx, Var var, String idx);

    Set<Var> getVars();

    Map<String, Set<String>> getIdxs(Var var);

    Map<String, String> getRevIdxs(Var var);

    Map<Var, Map<String, Set<String>>> getIdxs();

    Map<Var, Map<String, String>> getRevIdxs();

    Set<List<String>> getPatterns();

    void project(Collection<Var> vars);

    IdxTable join(IdxTable other);

    IdxTable union(IdxTable right);

    IdxTable leftOuterJoin(IdxTable right);

    IdxTable minus(IdxTable right);

}
