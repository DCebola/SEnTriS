package pt.fct.nova.id.srv.application.storage.idx_tables;

import org.apache.jena.sparql.core.Var;

import java.util.*;

public class MemIdxTable implements IdxTable {

    private final Map<Var, Map<String, List<String>>> idxs; //TODO: Need to address pairs/triples pointing to same idx
    private final Map<Var, Map<String, String>> rev_idxs;

    public MemIdxTable() {
        idxs = new HashMap<>();
        rev_idxs = new HashMap<>();
    }


    @Override
    public void addIdxs(String tableIdx, Map<Var, String> newIdxs) {
        newIdxs.forEach((v, i) -> saveIdx(i, v, tableIdx));
    }

    @Override
    public void addIdx(String tableIdx, Var var, String idx) {
        saveIdx(idx, var, tableIdx);
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
        idxs.put(var, v_idxs);
        rev_idxs.put(var, rev_v_idxs);

    }

    @Override
    public Map<String, String> getIdxs(Var var) {
        return idxs.get(var);
    }

    @Override
    public Map<String, String> getRevIdxs(Var var) {
        return rev_idxs.get(var);
    }

    @Override
    public Map<Var, Map<String, String>> getAll() {
        return idxs;
    }

    @Override
    public void project(Collection<Var> vars) {
        idxs.keySet().retainAll(vars);
        rev_idxs.keySet().retainAll(vars);
    }
}
