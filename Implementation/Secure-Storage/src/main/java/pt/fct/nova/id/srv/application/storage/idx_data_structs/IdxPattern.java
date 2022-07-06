package pt.fct.nova.id.srv.application.storage.idx_data_structs;

import org.apache.jena.sparql.core.Var;

import java.util.List;

public interface IdxPattern {
    List<Var> getVars();
    List<String> getIdxs();
    void addIdx(String idx);
    void addVar(Var var);
}
