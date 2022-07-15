package pt.fct.nova.id.srv.application.storage.iri_tables;

import org.apache.jena.sparql.core.Var;

import java.util.*;

import static pt.fct.nova.id.srv.application.Utils.generateID;

public class MemIRITable implements IRITable {

    private final Map<Var, Map<String, Set<String>>> iris;
    private final Map<Var, Map<String, String>> patterns;

    public MemIRITable() {
        iris = new HashMap<>();
        patterns = new HashMap<>();
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
    public void add(String patternIdx, Var var, String iri) {
        addIRI(iri, var, patternIdx);
        addPattern(iri, var, patternIdx);
    }

    private void addPattern(String iri, Var var, String patternIdx) {
        Map<String, String> v_p_idxs = patterns.get(var);
        if (v_p_idxs == null) {
            v_p_idxs = new HashMap<>();
            v_p_idxs.put(patternIdx, iri);
            patterns.put(var, v_p_idxs);
        } else
            v_p_idxs.put(patternIdx, iri);
    }

    private void addIRI(String iri, Var var, String patternIdx) {
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
            for (Var v2 : vars)
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
        Set<Var> mutual_vars = new HashSet<>(this.getVars());
        mutual_vars.retainAll(other.getVars());
        Set<String> diff = difference(this, other, mutual_vars);
        return cartesianProduct(mutual_vars, this, other, diff);
    }

    private Set<String> difference(IRITable left, IRITable right, Set<Var> vars) {
        Set<String> diff = new HashSet<>();
        Map<String, Set<String>> left_iris, right_iris;
        for (Var v : vars) {
            left_iris = left.getIRIs(v);
            right_iris = right.getIRIs(v);
            for (Map.Entry<String, Set<String>> entry : left_iris.entrySet()) {
                if (right_iris.get(entry.getKey()) == null)
                    diff.addAll(entry.getValue());
            }
            for (Map.Entry<String, Set<String>> entry : right_iris.entrySet()) {
                if (left_iris.get(entry.getKey()) == null)
                    diff.addAll(entry.getValue());
            }
        }
        return diff;
    }

    private IRITable cartesianProduct(Set<Var> mutualVars, IRITable left, IRITable right, Set<String> diff) {
        Set<Var> l_vars = new HashSet<>(left.getVars());
        Set<Var> r_vars = new HashSet<>(right.getVars());

        Set<Var> vars = new HashSet<>(l_vars);
        vars.addAll(r_vars);

        l_vars.removeAll(right.getVars());
        r_vars.removeAll(left.getVars());

        IRITable res = new MemIRITable(vars);

        Set<String> l_p_idxs, r_p_idxs;
        Map<String, Set<String>> iris_map, iris_map2;
        String iri, p_idx;

        for (Var v : mutualVars) {
            iris_map = left.getIRIs(v);
            iris_map2 = right.getIRIs(v);
            for (Map.Entry<String, Set<String>> entry : iris_map.entrySet()) {
                iri = entry.getKey();
                l_p_idxs = entry.getValue();
                r_p_idxs = iris_map2.get(iri);
                if (r_p_idxs != null) {
                    l_p_idxs.removeAll(diff);
                    r_p_idxs.removeAll(diff);
                    for (String p1 : l_p_idxs) {
                        for (String p2 : r_p_idxs) {
                            p_idx = generateID();
                            res.add(p_idx, v, iri);
                            for (Var v2 : l_vars)
                                res.add(p_idx, v2, left.getPatternIdxs(v2).get(p1));
                            for (Var v2 : r_vars)
                                res.add(p_idx, v2, right.getPatternIdxs(v2).get(p2));
                        }
                    }
                }
            }
        }
        return res;
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
