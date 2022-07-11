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

    /*
    @Override
    public void addIdxs(String patternIdx, Map<Var, String> newIdxs) {
        newIdxs.forEach((v, i) -> saveIdx(i, v, patternIdx));
    }
    */

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
        System.out.println("IDXTable");
        vars.forEach(System.out::println);
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
        System.out.println("JOIN");
        mutual_vars.forEach(System.out::println);
        System.out.println("-----");
        mutual_vars.retainAll(other.getVars());
        mutual_vars.forEach(System.out::println);

        Map<Var, Map<String, Set<String>>> join_idxs = new HashMap<>(indexes); //TODO: start empty and add only
        Map<Var, Map<String, String>> join_rev_idxs = new HashMap<>(rev_indexes); //TODO: start empty and add only

        Map<String, Set<String>> idx_map1, idx_map2;
        Map<String, String> rev_idx_map;
        String idx;
        Set<String> p_idxs_1, p_idxs_2;
        Set<String> idx_to_remove;

        for (Var var : mutual_vars) {
            idx_map1 = join_idxs.get(var); //
            rev_idx_map = join_rev_idxs.get(var);
            idx_map2 = other.getIdxs(var);
            idx_to_remove = new HashSet<>();
            for (Map.Entry<String, Set<String>> entry : idx_map1.entrySet()) {
                idx = entry.getKey();
                p_idxs_1 = entry.getValue();
                p_idxs_2 = idx_map2.get(idx);
                if (p_idxs_2 != null) {
                    //Add "other" values to join result
                    for (String p_idx : p_idxs_2) {
                        p_idxs_1.add(p_idx);
                        rev_idx_map.put(p_idx, idx);
                    }
                    //TODO: Add "other" value from non-mutual vars
                } else {
                    //TODO: delete this branch
                    //Remove "this" values from join result
                    idx_to_remove.add(idx);
                    for (String p_idx : p_idxs_1)
                        rev_idx_map.remove(p_idx);
                }
            }
            for (String s : idx_to_remove)
                idx_map1.remove(s);
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
