package pt.fct.nova.id.srv.application.storage.idx_tables;

import org.apache.jena.sparql.core.Var;

import java.util.Set;
import java.util.Map;

public interface IdxTable {

    void addIdxs(Map<Var, String> newIdxs);

    void addIdx(Var var, String idx);

    Map<String, String> getIdxs(Var var);

    Map<String, String> getRevIdxs(Var var);
}
