package pt.fct.nova.id.srv.application.storage.idx_tables;

import org.apache.jena.sparql.core.Var;

import java.util.Map;

public interface IdxTable2 extends IdxTable {

    void insertIdx2(String tableIdx1, String tableIdx2, String idx1, String idx2);

    Map<String, String> getIdxs2();

    Map<String, String> getRevIdxs2();

    Var getVar2();
}
