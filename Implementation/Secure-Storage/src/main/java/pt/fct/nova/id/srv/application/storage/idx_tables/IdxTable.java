package pt.fct.nova.id.srv.application.storage.idx_tables;

import org.apache.jena.sparql.core.Var;

import java.util.Collection;
import java.util.Map;

public interface IdxTable {

    void addIdxs(String tableIdx, Map<Var, String> newIdxs);

    void addIdx(String tableIdx, Var var, String idx);

    Map<String, String> getIdxs(Var var);

    Map<String, String> getRevIdxs(Var var);

    Map<Var, Map<String, String>> getAll();

    void project(Collection<Var> vars);
}
