package pt.fct.nova.id.srv.application.storage.idx_tables;

import org.apache.jena.sparql.core.Var;

import java.util.Map;

public interface IdxTable3 extends IdxTable2 {

    void insertIdx3(String tableIdx1, String tableIdx2, String tableIdx3, String idx1, String idx2, String idx3);

    Map<String, String> getIdxs3();

    Map<String, String> getRevIdxs3();

    Var getVar3();
}
