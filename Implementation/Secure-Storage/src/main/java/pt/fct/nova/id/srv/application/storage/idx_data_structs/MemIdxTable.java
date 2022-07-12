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
    public Map<Var, Map<String, Set<String>>> getIdxs() {
        return indexes;
    }

    @Override
    public Map<Var, Map<String, String>> getRevIdxs() {
        return rev_indexes;
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

        Set<Var> vars = this.getVars();
        Set<Var> vars2 = other.getVars();

        Set<Var> mutual_vars = new HashSet<>(vars);
        mutual_vars.retainAll(vars2);

        Set<Var> all_vars = new HashSet<>(vars);
        all_vars.addAll(vars2);

        vars.removeAll(this.getVars());
        vars2.removeAll(other.getVars());

        Map<Var, Map<String, Set<String>>> join_idxs = new HashMap<>();
        Map<Var, Map<String, String>> join_rev_idxs = new HashMap<>();

        for (Var v : all_vars) {
            join_idxs.put(v, new HashMap<>());
            join_rev_idxs.put(v, new HashMap<>());
        }

        Set<String> patterns_to_remove = execIdxsJoin(other, vars, vars2, mutual_vars, join_idxs, join_rev_idxs);
        linkPatternsToIdxs(all_vars, join_idxs, join_rev_idxs, patterns_to_remove);

        return new MemIdxTable(join_idxs, join_rev_idxs);
    }

    private void linkPatternsToIdxs(Set<Var> all_vars, Map<Var, Map<String, Set<String>>> join_idxs, Map<Var, Map<String, String>> join_rev_idxs, Set<String> patterns_to_remove) {
        Map<String, String> p_map;
        Map<String, Set<String>> idx_map;
        String idx;
        for (Var v : all_vars) {
            p_map = join_rev_idxs.get(v);
            for (String p_idx : p_map.keySet()) {
                idx_map = join_idxs.get(v);
                idx = p_map.get(p_idx);
                if (patterns_to_remove.contains(p_idx))
                    idx_map.remove(idx);
                else
                    idx_map.get(idx).add(p_idx);
            }
        }
    }

    private Set<String> execIdxsJoin(IdxTable other, Set<Var> vars, Set<Var> vars2, Set<Var> mutual_vars, Map<Var, Map<String, Set<String>>> join_idxs, Map<Var, Map<String, String>> join_rev_idxs) {
        Set<String> p_idxs2, p_idxs;
        Map<String, Set<String>> idx_map, idx_map2;
        String idx;
        Set<String> patterns_to_remove = new HashSet<>();
        for (Var v : mutual_vars) {
            idx_map = indexes.get(v);
            idx_map2 = other.getIdxs(v);
            for (Map.Entry<String, Set<String>> entry : idx_map.entrySet()) {
                idx = entry.getKey();
                p_idxs = entry.getValue();
                p_idxs2 = idx_map2.get(idx);
                if (p_idxs2 != null) {
                    join_idxs.get(v).put(idx, new HashSet<>());
                    saveMatchingIdx(v, idx, join_idxs, join_rev_idxs, indexes, rev_indexes, vars, p_idxs);
                    saveMatchingIdx(v, idx, join_idxs, join_rev_idxs, other.getIdxs(), other.getRevIdxs(), vars2, p_idxs2);
                } else
                    patterns_to_remove.addAll(p_idxs);
            }
            for (Map.Entry<String, Set<String>> entry : idx_map2.entrySet()) {
                p_idxs2 = entry.getValue();
                p_idxs = idx_map.get(entry.getKey());
                if (p_idxs == null)
                    patterns_to_remove.addAll(p_idxs2);
            }
        }
        return patterns_to_remove;
    }

    private void saveMatchingIdx(Var v, String idx, Map<Var, Map<String, Set<String>>> join_idxs, Map<Var, Map<String, String>> join_rev_idxs, Map<Var, Map<String, Set<String>>> idxs, Map<Var, Map<String, String>> rev_idxs, Set<Var> vars, Set<String> p_idxs) {
        for (String p_idx : p_idxs)
            join_rev_idxs.get(v).put(p_idx, idx);
        for (Var v2 : vars) {
            if (v2 != v) {
                for (String p_idx : p_idxs) {
                    idx = rev_idxs.get(v2).get(p_idx);
                    join_idxs.get(v2).put(idx, new HashSet<>());
                    for (String p_idx2 : idxs.get(v2).get(idx))
                        join_rev_idxs.get(v2).put(p_idx2, idx);
                }
            }
        }
    }

    private void saveMatchingIdx() {
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
