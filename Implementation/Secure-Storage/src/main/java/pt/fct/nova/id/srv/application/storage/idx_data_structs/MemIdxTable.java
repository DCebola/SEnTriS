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

        Set<Var> vars = new HashSet<>(this.getVars());
        Set<Var> vars2 = new HashSet<>(other.getVars());

        Set<Var> mutual_vars = new HashSet<>(vars);
        mutual_vars.retainAll(vars2);

        Set<Var> all_vars = new HashSet<>(vars);
        all_vars.addAll(vars2);

        vars.removeAll(other.getVars());
        vars2.removeAll(this.getVars());

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
        Set<String> p_idxs;
        String idx;
        for (Var v : all_vars) {
            p_map = join_rev_idxs.get(v);
            for (String p_idx : p_map.keySet()) {
                if (patterns_to_remove.contains(p_idx)) {
                    idx_map = join_idxs.get(v);
                    idx = p_map.get(p_idx);
                    p_idxs = idx_map.get(idx);
                    if (p_idxs != null) {
                        p_idxs.remove(p_idx);
                        System.out.println("IDX to remove: i->" + idx + ", p->" + p_idx);
                        if (p_idxs.isEmpty())
                            idx_map.remove(idx);
                    }
                }
            }
        }
        for (Var v : all_vars) {
            p_map = join_rev_idxs.get(v);
            for (String p_idx : patterns_to_remove)
                p_map.remove(p_idx);
        }

    }

    private Set<String> execIdxsJoin(IdxTable other, Set<Var> vars, Set<Var> vars2, Set<Var> mutual_vars, Map<Var, Map<String, Set<String>>> join_idxs, Map<Var, Map<String, String>> join_rev_idxs) {
        Set<String> p_idxs2, p_idxs, join_p_idxs;
        Map<String, Set<String>> idx_map, idx_map2;
        Map<String, String> v_rev_idx;
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
                    join_p_idxs = new HashSet<>(p_idxs);
                    join_p_idxs.addAll(p_idxs2);
                    join_idxs.get(v).put(idx, join_p_idxs);
                    System.out.println("[" + v + "] - MATCH for [" + idx + "]: adding p_idxs -> " + Arrays.toString(join_p_idxs.toArray()));
                    v_rev_idx = join_rev_idxs.get(v);
                    for (String p_idx : join_p_idxs)
                        v_rev_idx.put(p_idx, idx);
                    saveIdxsFromOtherVars(join_idxs, join_rev_idxs, indexes, rev_indexes, vars, p_idxs);
                    saveIdxsFromOtherVars(join_idxs, join_rev_idxs, other.getIdxs(), other.getRevIdxs(), vars2, p_idxs2);
                } else {
                    System.out.println("[" + v + "] - 2: NO MATCH for [" + idx + "]: adding to remove set -> " + Arrays.toString(p_idxs.toArray()));
                    patterns_to_remove.addAll(p_idxs);

                }
            }
            for (Map.Entry<String, Set<String>> entry : idx_map2.entrySet()) {
                idx = entry.getKey();
                p_idxs2 = entry.getValue();
                p_idxs = idx_map.get(idx);
                if (p_idxs == null) {
                    System.out.println("[" + v + "] - 1: NO MATCH for [" + idx + "]: adding to remove set -> " + Arrays.toString(p_idxs2.toArray()));
                    patterns_to_remove.addAll(p_idxs2);
                }
            }
        }
        return patterns_to_remove;
    }

    private void saveIdxsFromOtherVars(Map<Var, Map<String, Set<String>>> join_idxs, Map<Var, Map<String, String>> join_rev_idxs, Map<Var, Map<String, Set<String>>> idxs, Map<Var, Map<String, String>> rev_idxs, Set<Var> vars, Set<String> p_idxs) {
        String idx2;
        Set<String> p_idxs2;
        for (Var v : vars) {
            for (String p_idx : p_idxs) {
                idx2 = rev_idxs.get(v).get(p_idx);
                p_idxs2 = new HashSet<>(idxs.get(v).get(idx2));
                join_idxs.get(v).put(idx2, p_idxs2);
                for (String p_idx2 : p_idxs2)
                    join_rev_idxs.get(v).put(p_idx2, idx2);
            }
        }
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
