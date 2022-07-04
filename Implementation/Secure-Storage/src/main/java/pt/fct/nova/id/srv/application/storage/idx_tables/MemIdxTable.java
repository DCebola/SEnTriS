package pt.fct.nova.id.srv.application.storage.idx_tables;

import org.apache.jena.sparql.core.Var;

import java.util.*;

import static pt.fct.nova.id.srv.application.Utils.generateID;

public class MemIdxTable implements IdxTable {

    private final Map<Var, Map<String, String>> idxs;
    private final Map<Var, Map<String, String>> rev_idxs;

    public MemIdxTable() {
        idxs = new HashMap<>();
        rev_idxs = new HashMap<>();
    }

    @Override
    public void addIdxs(Map<Var, String> newIdxs) {
        String t_idx = generateID();
        newIdxs.forEach((v, i) -> saveIdx(i, v, t_idx));
    }

    @Override
    public void addIdx(Var var, String idx) {
        String t_idx = generateID();
        saveIdx(idx, var, t_idx);
    }

    private void saveIdx(String idx, Var var, String tableIdx) {
        Map<String, String> v_idxs = idxs.get(var);
        Map<String, String> rev_v_idxs = rev_idxs.get(var);
        if (v_idxs == null) {
            v_idxs = new HashMap<>();
            rev_v_idxs = new HashMap<>();
        }
        v_idxs.put(idx, tableIdx);
        rev_v_idxs.put(tableIdx, idx);
    }

    @Override
    public Map<String, String> getIdxs(Var var) {
        return idxs.get(var);
    }

    @Override
    public Map<String, String> getRevIdxs(Var var) {
        return rev_idxs.get(var);
    }
}
