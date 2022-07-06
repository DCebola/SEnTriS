package pt.fct.nova.id.srv.application.storage.idx_data_structs;

import org.apache.jena.sparql.core.Var;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IdxTable {

    void addIdx(String tripleIdx, Var var, String idx);

    Map<String, Set<String>> getIdxs(Var var);

    Map<String, String> getRevIdxs(Var var);

    List<IdxPattern> getPatterns();

    void project(Collection<Var> vars);
}
