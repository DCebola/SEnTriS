package pt.fct.nova.id.srv.application.storage.idx_data_structs;

import org.apache.jena.sparql.core.Var;

import java.lang.reflect.Array;
import java.util.*;

public class MemIdxTable implements IdxTable {

    private final Map<Var, Map<String, Set<String>>> idxs;
    private final Map<Var, Map<String, String>> rev_idxs;

    public MemIdxTable() {
        idxs = new HashMap<>();
        rev_idxs = new HashMap<>();
    }

    /*
    @Override
    public void addIdxs(String patternIdx, Map<Var, String> newIdxs) {
        newIdxs.forEach((v, i) -> saveIdx(i, v, patternIdx));
    }
    */

    @Override
    public void addIdx(String patternIdx, Var var, String idx) {
        saveIdx(idx, var, patternIdx);
    }

    private void saveIdx(String idx, Var var, String patternIdx) {
        Map<String, Set<String>> v_idxs = idxs.get(var);
        Map<String, String> r_v_idxs = rev_idxs.get(var);

        if (v_idxs == null) {
            v_idxs = new HashMap<>();
            r_v_idxs = new HashMap<>();
            Set<String> p_idxs = new HashSet<>();
            p_idxs.add(patternIdx);
            v_idxs.put(idx, p_idxs);
            r_v_idxs.put(patternIdx, idx);
            idxs.put(var, v_idxs);
            rev_idxs.put(var, r_v_idxs);
        } else {
            v_idxs.get(idx).add(patternIdx);
            r_v_idxs.put(patternIdx, idx);
        }
    }

    @Override
    public Map<String, Set<String>> getIdxs(Var var) {
        return idxs.get(var);
    }

    @Override
    public Map<String, String> getRevIdxs(Var var) {
        return rev_idxs.get(var);
    }

    @Override
    public List<IdxPattern> getAll() {
        List<IdxPattern> res = new LinkedList<>();
        IdxPattern pattern;
        Set<Var> vars = rev_idxs.keySet();
        if (vars.isEmpty())
            return res;
        Var v = vars.iterator().next();
        Set<String> p_idxs = rev_idxs.get(v).keySet();
        for (String p_idx : p_idxs) {
            pattern = new MemIdxPattern();
            for (Var v2 : rev_idxs.keySet()) {
                pattern.addVar(v2);
                pattern.addIdx(rev_idxs.get(v2).get(p_idx));
            }
            res.add(pattern);
        }
        return res;
    }

    @Override
    public void project(Collection<Var> vars) {
        idxs.keySet().retainAll(vars);
        rev_idxs.keySet().retainAll(vars);
    }
}
