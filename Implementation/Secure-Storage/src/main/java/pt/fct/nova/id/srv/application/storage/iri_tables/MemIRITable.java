package pt.fct.nova.id.srv.application.storage.iri_tables;

import org.apache.jena.sparql.core.Var;

import java.util.*;

public class MemIRITable implements IRITable {

    private final Map<Var, Map<String, Set<String>>> iris;
    private final Map<Var, Map<String, String>> patterns;

    public MemIRITable() {
        iris = new HashMap<>();
        patterns = new HashMap<>();
    }

    public MemIRITable(Map<Var, Map<String, Set<String>>> iris, Map<Var, Map<String, String>> patterns) {
        this.iris = iris;
        this.patterns = patterns;
    }

    public MemIRITable(Set<Var> vars) {
        iris = new HashMap<>();
        patterns = new HashMap<>();
        for (Var v : vars) {
            iris.put(v, new HashMap<>());
            patterns.put(v, new HashMap<>());
        }
    }

    @Override
    public void addIRI(String patternIdx, Var var, String iri) {
        saveIRI(iri, var, patternIdx);
        savePattern(iri, var, patternIdx);
    }

    private void savePattern(String iri, Var var, String patternIdx) {
        Map<String, String> v_p_idx = patterns.get(var);
        if (v_p_idx == null) {
            v_p_idx = new HashMap<>();
            v_p_idx.put(patternIdx, iri);
            patterns.put(var, v_p_idx);
        } else
            v_p_idx.put(patternIdx, iri);
    }

    private void saveIRI(String iri, Var var, String patternIdx) {
        Map<String, Set<String>> v_iris = iris.get(var);
        if (v_iris == null) {
            v_iris = new HashMap<>();
            savePatternIdxs(v_iris, iri, patternIdx);
            iris.put(var, v_iris);
        } else {
            Set<String> p_idxs = v_iris.get(iri);
            if (p_idxs == null)
                savePatternIdxs(v_iris, iri, patternIdx);
            else
                p_idxs.add(patternIdx);
        }
    }

    private void savePatternIdxs(Map<String, Set<String>> varIRIs, String iri, String patternIdx) {
        Set<String> p_idxs = new HashSet<>();
        p_idxs.add(patternIdx);
        varIRIs.put(iri, p_idxs);
    }

    @Override
    public Set<Var> getVars() {
        return iris.keySet();
    }

    @Override
    public Map<String, Set<String>> getIRIs(Var var) {
        return iris.get(var);
    }

    @Override
    public Map<String, String> getPatternIdxs(Var var) {
        return patterns.get(var);
    }

    @Override
    public Map<Var, Map<String, Set<String>>> getIRIs() {
        return iris;
    }

    @Override
    public Map<Var, Map<String, String>> getPatternIdxs() {
        return patterns;
    }

    @Override
    public Set<List<String>> getPatterns() {
        Set<List<String>> res = new HashSet<>();
        List<String> pattern;
        Set<Var> vars = patterns.keySet();
        if (vars.isEmpty())
            return res;
        Var v = vars.iterator().next();
        Set<String> p_idxs = patterns.get(v).keySet();
        for (String p_idx : p_idxs) {
            pattern = new ArrayList<>(vars.size());
            for (Var v2 : patterns.keySet())
                pattern.add(patterns.get(v2).get(p_idx));
            res.add(pattern);
        }
        return res;
    }

    @Override
    public void project(Collection<Var> vars) {
        iris.keySet().retainAll(vars);
        patterns.keySet().retainAll(vars);
    }

    @Override
    public IRITable join(IRITable other) {

        Set<Var> vars = new HashSet<>(this.getVars());
        Set<Var> vars2 = new HashSet<>(other.getVars());

        Set<Var> mutual_vars = new HashSet<>(vars);
        mutual_vars.retainAll(vars2);

        Set<Var> all_vars = new HashSet<>(vars);
        all_vars.addAll(vars2);

        vars.removeAll(other.getVars());
        vars2.removeAll(this.getVars());

        Map<Var, Map<String, Set<String>>> join_iris = new HashMap<>();
        Map<Var, Map<String, String>> join_pattern_idxs = new HashMap<>();

        for (Var v : all_vars) {
            join_iris.put(v, new HashMap<>());
            join_pattern_idxs.put(v, new HashMap<>());
        }

        Set<String> p_idxs_to_remove = joinIRIs(other, vars, vars2, mutual_vars, join_iris, join_pattern_idxs);
        cleanPatternIdxs(all_vars, join_iris, join_pattern_idxs, p_idxs_to_remove);

        return new MemIRITable(join_iris, join_pattern_idxs);
    }

    private void cleanPatternIdxs(Set<Var> allVars, Map<Var, Map<String, Set<String>>> joinIRIs, Map<Var, Map<String, String>> joinPatternIdxs, Set<String> patternIdxsToRemove) {
        Map<String, String> p_map;
        Map<String, Set<String>> iris_map;
        Set<String> p_idxs;
        String iri;
        for (Var v : allVars) {
            p_map = joinPatternIdxs.get(v);
            for (String p_idx : p_map.keySet()) {
                if (patternIdxsToRemove.contains(p_idx)) {
                    iris_map = joinIRIs.get(v);
                    iri = p_map.get(p_idx);
                    p_idxs = iris_map.get(iri);
                    if (p_idxs != null) {
                        p_idxs.remove(p_idx);
                        System.out.println("[" + iri + "] - Removed: " + p_idx);
                        if (p_idxs.isEmpty())
                            iris_map.remove(iri);
                    }
                }
            }
        }
        for (Var v : allVars) {
            p_map = joinPatternIdxs.get(v);
            for (String p_idx : patternIdxsToRemove)
                p_map.remove(p_idx);
        }

    }

    private Set<String> joinIRIs(IRITable other, Set<Var> vars, Set<Var> vars2, Set<Var> mutualVars, Map<Var, Map<String, Set<String>>> joinIRIs, Map<Var, Map<String, String>> joinPatternIdxs) {
        Set<String> p_idxs2, p_idxs, join_p_idxs;
        Map<String, Set<String>> iris_map, iris_map2;
        Map<String, String> v_p_idx;
        String iri;
        Set<String> patterns_to_remove = new HashSet<>();
        for (Var v : mutualVars) {
            iris_map = iris.get(v);
            iris_map2 = other.getIRIs(v);
            for (Map.Entry<String, Set<String>> entry : iris_map.entrySet()) {
                iri = entry.getKey();
                p_idxs = entry.getValue();
                p_idxs2 = iris_map2.get(iri);
                if (p_idxs2 != null) {
                    join_p_idxs = new HashSet<>(p_idxs);
                    join_p_idxs.addAll(p_idxs2);
                    joinIRIs.get(v).put(iri, join_p_idxs);
                    System.out.println("[" + v + "] - MATCH for [" + iri + "]: adding p_idxs -> " + Arrays.toString(join_p_idxs.toArray()));
                    v_p_idx = joinPatternIdxs.get(v);
                    for (String p_idx : join_p_idxs)
                        v_p_idx.put(p_idx, iri);
                    saveIRISFromNonMutualVars(joinIRIs, joinPatternIdxs, iris, patterns, vars, p_idxs);
                    saveIRISFromNonMutualVars(joinIRIs, joinPatternIdxs, other.getIRIs(), other.getPatternIdxs(), vars2, p_idxs2);
                } else {
                    System.out.println("[" + v + "] - 2: NO MATCH for [" + iri + "]: adding to remove set -> " + Arrays.toString(p_idxs.toArray()));
                    patterns_to_remove.addAll(p_idxs);

                }
            }
            for (Map.Entry<String, Set<String>> entry : iris_map2.entrySet()) {
                iri = entry.getKey();
                p_idxs2 = entry.getValue();
                p_idxs = iris_map.get(iri);
                if (p_idxs == null) {
                    System.out.println("[" + v + "] - 1: NO MATCH for [" + iri + "]: adding to remove set -> " + Arrays.toString(p_idxs2.toArray()));
                    patterns_to_remove.addAll(p_idxs2);
                }
            }
        }
        return patterns_to_remove;
    }

    private void saveIRISFromNonMutualVars(Map<Var, Map<String, Set<String>>> joinIRIs, Map<Var, Map<String, String>> joinPatternIdxs, Map<Var, Map<String, Set<String>>> iris, Map<Var, Map<String, String>> patterns, Set<Var> vars, Set<String> patternIdxs) {
        String idx2;
        Set<String> p_idxs2;
        for (Var v : vars) {
            for (String p_idx : patternIdxs) {
                idx2 = patterns.get(v).get(p_idx);
                p_idxs2 = new HashSet<>(iris.get(v).get(idx2));
                joinIRIs.get(v).put(idx2, p_idxs2);
                for (String p_idx2 : p_idxs2)
                    joinPatternIdxs.get(v).put(p_idx2, idx2);
            }
        }
    }


    @Override
    public IRITable union(IRITable right) {
        return null;
    }

    @Override
    public IRITable leftOuterJoin(IRITable right) {
        return null;
    }

    @Override
    public IRITable minus(IRITable right) {
        return null;
    }

}
