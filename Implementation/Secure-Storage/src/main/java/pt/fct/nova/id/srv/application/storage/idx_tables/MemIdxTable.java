package pt.fct.nova.id.srv.application.storage.idx_tables;

import org.apache.jena.sparql.core.Var;

import java.util.*;

public class MemIdxTable implements IdxTable {

    private final Var var1;
    private final Map<String, String> idxs1;
    private final Map<String, String> rev_idxs1;

    public MemIdxTable(Var var) {
        var1 = var;
        idxs1 = new HashMap<>();
        rev_idxs1 = new HashMap<>();
    }

    @Override
    public void insertIdx1(String tableIdx, String idx) {
        idxs1.put(idx, tableIdx);
        rev_idxs1.put(tableIdx, idx);
    }

    @Override
    public Map<String, String> getIdxs1() {
        return idxs1;
    }

    @Override
    public Map<String, String> getRevIdxs1() {
        return rev_idxs1;
    }

    public Var getVar1() {
        return var1;
    }
}
