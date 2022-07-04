package pt.fct.nova.id.srv.application.storage.idx_tables;

import org.apache.jena.sparql.core.Var;

import java.util.List;
import java.util.Map;

public interface IdxTable {

    void insertIdx1(String tableIdx, String idx);

    Map<String, String> getIdxs1();

    Map<String, String> getRevIdxs1();

    Var getVar1();
}
