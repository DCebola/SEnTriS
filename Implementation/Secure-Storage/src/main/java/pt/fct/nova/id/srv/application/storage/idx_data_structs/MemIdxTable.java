package pt.fct.nova.id.srv.application.storage.idx_data_structs;

import org.apache.jena.sparql.core.Var;

import java.util.*;

public class MemIdxTable implements IdxTable {

    private final Map<Var, Map<String, Set<String>>> indexes;
    private final Map<Var, Map<String, String>> rev_indexes;

    public MemIdxTable() {
        indexes = new HashMap<>();
        rev_indexes = new HashMap<>();
    }

    public MemIdxTable(Map<Var, Map<String, Set<String>>> indexes, Map<Var, Map<String, String>> rev_indexes) {
        this.indexes = indexes;
        this.rev_indexes = rev_indexes;
    }

    public MemIdxTable(Set<Var> vars) {
        indexes = new HashMap<>();
        rev_indexes = new HashMap<>();
        for (Var v : vars) {
            indexes.put(v, new HashMap<>());
            rev_indexes.put(v, new HashMap<>());
        }
    }

    @Override
    public void addIdx(String patternIdx, Var var, String idx) {
        saveIdx(idx, var, patternIdx);
        saveRevIdx(idx, var, patternIdx);
    }

    private void saveRevIdx(String idx, Var var, String patternIdx) {
        Map<String, String> r_v_idxs = rev_indexes.get(var);
        if (r_v_idxs == null) {
            r_v_idxs = new HashMap<>();
            r_v_idxs.put(patternIdx, idx);
            rev_indexes.put(var, r_v_idxs);
        } else
            r_v_idxs.put(patternIdx, idx);
    }

    private void saveIdx(String idx, Var var, String patternIdx) {
        Map<String, Set<String>> v_idxs = indexes.get(var);
        if (v_idxs == null) {
            v_idxs = new HashMap<>();
            savePatternIdxs(v_idxs, idx, patternIdx);
            indexes.put(var, v_idxs);
        } else {
            Set<String> p_idxs = v_idxs.get(idx);
            if (p_idxs == null)
                savePatternIdxs(v_idxs, idx, patternIdx);
            else
                p_idxs.add(patternIdx);
        }
    }

    private void savePatternIdxs(Map<String, Set<String>> vIdxs, String idx, String patternIdx) {
        Set<String> p_idxs = new HashSet<>();
        p_idxs.add(patternIdx);
        vIdxs.put(idx, p_idxs);
    }

    @Override
    public Set<Var> getVars() {
        return indexes.keySet();
    }

    @Override
    public Map<String, Set<String>> getIdxs(Var var) {
        return indexes.get(var);
    }

    @Override
    public Map<String, String> getRevIdxs(Var var) {
        return rev_indexes.get(var);
    }

    @Override
    public Set<List<String>> getPatterns() {
        Set<List<String>> res = new HashSet<>();
        List<String> pattern;
        Set<Var> vars = rev_indexes.keySet();
        if (vars.isEmpty())
            return res;
        Var v = vars.iterator().next();
        Set<String> p_idxs = rev_indexes.get(v).keySet();
        for (String p_idx : p_idxs) {
            pattern = new ArrayList<>(vars.size());
            for (Var v2 : rev_indexes.keySet())
                pattern.add(rev_indexes.get(v2).get(p_idx));
            res.add(pattern);
        }
        return res;
    }

    @Override
    public void project(Collection<Var> vars) {
        indexes.keySet().retainAll(vars);
        rev_indexes.keySet().retainAll(vars);
    }

    @Override
    public IdxTable join(IdxTable other) {

        Set<Var> mutual_vars = new HashSet<>(getVars());
        mutual_vars.retainAll(other.getVars());

        Set<Var> vars = new HashSet<>(getVars());
        Set<Var> vars2 = new HashSet<>(other.getVars());
        vars.removeAll(mutual_vars);
        vars2.removeAll(mutual_vars);

        Map<Var, Map<String, Set<String>>> join_idxs = new HashMap<>(indexes); //TODO: start with empty maps
        Map<Var, Map<String, String>> join_rev_idxs = new HashMap<>(rev_indexes);

        for (Var v : vars2) {
            join_idxs.put(v, new HashMap<>());
            join_rev_idxs.put(v, new HashMap<>());
        }

        Map<String, Set<String>> idx_map, idx_map2;
        Map<String, String> rev_idx_map;
        String idx;
        Set<String> p_idxs, p_idxs2;
        Set<String> idxs_to_remove;

        for (Var v : mutual_vars) {
            idx_map = join_idxs.get(v); //
            rev_idx_map = join_rev_idxs.get(v);
            idx_map2 = other.getIdxs(v);
            idxs_to_remove = new HashSet<>();
            for (Map.Entry<String, Set<String>> entry : idx_map.entrySet()) {
                idx = entry.getKey();
                p_idxs = entry.getValue();
                p_idxs2 = idx_map2.get(idx);
                if (p_idxs2 != null) {
                    //Add "other" values to mutual vars
                    for (String p_idx : p_idxs2) {
                        p_idxs.add(p_idx);
                        rev_idx_map.put(p_idx, idx);
                    }
                    //Add "other" value from non-mutual vars
                    for (Var v2 : vars2) {
                        for (String p_idx : p_idxs2) {
                            idx = other.getRevIdxs(v2).get(p_idx);
                            idx_map2 = join_idxs.get(v2);
                            p_idxs = join_idxs.get(v2).get(idx);
                            if (p_idxs == null)
                                savePatternIdxs(idx_map2, idx, p_idx);
                            else
                                p_idxs.add(p_idx);
                            join_rev_idxs.get(v2).put(p_idx, idx);
                        }
                    }
                } else {
                    //TODO: Only add, need to drop remove approach;
                    //Remove "this" values from mutual vars
                    idxs_to_remove.add(idx);
                    for (String p_idx : p_idxs)
                        rev_idx_map.remove(p_idx);

                    //Remove "this" values from non-mutual vars
                    for (Var v2 : vars) {
                        for (String p_idx : p_idxs) {
                            idx = join_rev_idxs.get(v2).remove(p_idx);
                            if (idx != null)
                                idxs_to_remove.add(idx);
                        }
                    }
                }
            }
            for (String s : idxs_to_remove)
                idx_map.remove(s);
        }
        return new MemIdxTable(join_idxs, join_rev_idxs);
    }


    @Override
    public IdxTable union(IdxTable right) {
        return null;
    }

    @Override
    public IdxTable leftOuterJoin(IdxTable right) {
        return null;
    }

    @Override
    public IdxTable minus(IdxTable right) {
        return null;
    }

}
