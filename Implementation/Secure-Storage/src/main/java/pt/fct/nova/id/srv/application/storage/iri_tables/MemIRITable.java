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
        //TODO: Need to use list instead of set, results need to have duplicates if they exist
        //TODO: DISTINCT -> use set
        Set<List<String>> res = new HashSet<>();
        List<String> pattern;
        Set<Var> vars = patterns.keySet();
        if (vars.isEmpty())
            return res;
        Var v = vars.iterator().next();
        Set<String> p_idxs = patterns.get(v).keySet();
        String iri;
        int i;
        for (String p_idx : p_idxs) {
            pattern = new ArrayList<>(vars.size());
            i = 0;
            for (Var v2 : vars) {
                iri = patterns.get(v2).get(p_idx);
                pattern.add(iri);
                if (iri == null)
                    i++;
            }
            if (i < vars.size())
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
        Set<String> filter = getIncompatiblePatterns(this, other, mutual_vars);
        return join(mutual_vars, this, other, filter);
    }

    private Set<String> getIncompatiblePatterns(IRITable left, IRITable right, Set<Var> vars) {
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


    private IRITable join(Set<Var> mutualVars, IRITable left, IRITable right, Set<String> filter) {
        Set<Var> l_vars = new HashSet<>(left.getVars());
        Set<Var> r_vars = new HashSet<>(right.getVars());

        Set<Var> vars = new HashSet<>(l_vars);
        vars.addAll(r_vars);

        l_vars.removeAll(right.getVars());
        r_vars.removeAll(left.getVars());

        IRITable res = new MemIRITable(vars);

        Set<String> l_p_idxs, r_p_idxs;
        Map<String, Set<String>> iris_map, iris_map2;
        String iri;

        for (Var v : mutualVars) {
            iris_map = left.getIRIs(v);
            iris_map2 = right.getIRIs(v);
            for (Map.Entry<String, Set<String>> entry : iris_map.entrySet()) {
                iri = entry.getKey();
                l_p_idxs = entry.getValue();
                r_p_idxs = iris_map2.get(iri);
                if (r_p_idxs != null) {
                    l_p_idxs.removeAll(filter);
                    r_p_idxs.removeAll(filter);
                    innerJoinPatterns(v, iri, mutualVars, left, l_vars, l_p_idxs, right, r_vars, r_p_idxs, res);
                }
            }
            break;
        }
        return res;
    }

    private void innerJoinPatterns(Var currentVar, String iri, Set<Var> mutualVars, IRITable left, Set<Var> leftVars, Set<String> leftPatternIdxs, IRITable right,
                                   Set<Var> rightVars, Set<String> rightPatternIdxs, IRITable res) {
        String p_idx, iri2;
        for (String p1 : leftPatternIdxs) {
            for (String p2 : rightPatternIdxs) {
                if (equalPatterns(mutualVars, currentVar, left, p1, right, p2)) {
                    p_idx = generateID();
                    res.add(p_idx, currentVar, iri);
                    for (Var v2 : mutualVars) {
                        if (v2 != currentVar) {
                            iri2 = left.getPatternIdxs(v2).get(p1);
                            if (iri2 != null)
                                res.add(p_idx, v2, iri2);
                        }
                    }
                    for (Var v2 : leftVars) {
                        iri2 = left.getPatternIdxs(v2).get(p1);
                        if (iri2 != null)
                            res.add(p_idx, v2, iri2);
                    }
                    for (Var v2 : rightVars) {
                        iri2 = right.getPatternIdxs(v2).get(p2);
                        if (iri2 != null)
                            res.add(p_idx, v2, iri2);
                    }
                }
            }
        }
    }

    private boolean equalPatterns(Set<Var> mutualVars, Var currentVar, IRITable left, String leftPattern, IRITable right, String rightPattern) {
        for (Var v2 : mutualVars) {
            if (v2 != currentVar)
                if (!left.getPatternIdxs(v2).get(leftPattern).equals(right.getPatternIdxs(v2).get(rightPattern)))
                    return false;
        }
        return true;
    }

    @Override
    public IRITable union(IRITable other) {

        Set<Var> l_vars = this.getVars();
        Set<Var> r_vars = other.getVars();

        Set<Var> mutual_vars = new HashSet<>(l_vars);
        mutual_vars.retainAll(r_vars);

        Set<Var> vars = new HashSet<>(l_vars);
        vars.addAll(r_vars);

        IRITable res = new MemIRITable(vars);

        Set<String> l_p_idxs, r_p_idxs;
        String iri;
        for (Var v : mutual_vars) {
            for (Map.Entry<String, Set<String>> entry : this.getIRIs(v).entrySet()) {
                iri = entry.getKey();
                l_p_idxs = entry.getValue();
                for (String p : l_p_idxs) {
                    res.add(p, v, iri);
                    addOtherVars(p, l_vars, v, this, res);
                }
            } //TODO: Need to verify that res does not have an equal pattern
            for (Map.Entry<String, Set<String>> entry : other.getIRIs(v).entrySet()) {
                iri = entry.getKey();
                r_p_idxs = entry.getValue();
                for (String p : r_p_idxs) {
                    res.add(p, v, iri);
                    addOtherVars(p, r_vars, v, other, res);
                }
            }
            break;
        }
        return res;
    }

    private void addOtherVars(String pattern, Set<Var> mutual_vars, Var v, IRITable source, IRITable target) {
        for (Var v2 : mutual_vars) {
            if (v2 != v) {
                String iri = source.getPatternIdxs(v2).get(pattern);
                if (iri != null)
                    target.add(pattern, v2, iri);
            }
        }
    }

    @Override
    public IRITable leftOuterJoin(IRITable other) {
        Set<Var> mutual_vars = new HashSet<>(this.getVars());
        mutual_vars.retainAll(other.getVars());
        return leftOuterJoin(mutual_vars, this, other);
    }

    private IRITable leftOuterJoin(Set<Var> mutualVars, MemIRITable left, IRITable right) {
        //TODO: Current implementation wrong, need to update using an adapted version of the inner join (change the equality method on patterns)
        Set<Var> l_vars = new HashSet<>(left.getVars());
        Set<Var> r_vars = new HashSet<>(right.getVars());
        System.out.println("LEFT: " + Arrays.toString(l_vars.toArray()));
        System.out.println("RIGHT: " + Arrays.toString(r_vars.toArray()));

        Set<Var> vars = new HashSet<>(l_vars);
        vars.addAll(r_vars);

        l_vars.removeAll(right.getVars());
        r_vars.removeAll(left.getVars());

        IRITable res = new MemIRITable(vars);

        Set<String> l_p_idxs, r_p_idxs;
        Map<String, Set<String>> iris_map, iris_map2;
        String iri;

        for (Var v : mutualVars) {
            iris_map = left.getIRIs(v);
            iris_map2 = right.getIRIs(v);
            for (Map.Entry<String, Set<String>> entry : iris_map.entrySet()) {
                iri = entry.getKey();
                l_p_idxs = entry.getValue();
                r_p_idxs = iris_map2.get(iri);
                if (r_p_idxs != null)
                    innerJoinPatterns(v, iri, mutualVars, left, l_vars, l_p_idxs, right, r_vars, r_p_idxs, res);
                else {
                    for (String p : l_p_idxs) {
                        res.add(p, v, iri);
                        addOtherVars(p, l_vars, v, left, res);
                    }
                }
            }
            break;
        }
        return res;


    }

    @Override
    public IRITable minus(IRITable other) {
        Set<Var> mutual_vars = new HashSet<>(this.getVars());
        mutual_vars.retainAll(other.getVars());

        Set<String> diff = getIncompatiblePatterns(this, other, mutual_vars);

        IRITable res = new MemIRITable(mutual_vars);
        Set<String> p_idxs;
        Map<String, Set<String>> iris_map;
        String iri;
        for (Var v : mutual_vars) {
            iris_map = this.getIRIs(v);
            for (Map.Entry<String, Set<String>> entry : iris_map.entrySet()) {
                iri = entry.getKey();
                p_idxs = entry.getValue();
                for (String p : p_idxs) {
                    if (diff.contains(p)) {
                        res.add(p, v, iri);
                        addOtherVars(p, mutual_vars, v, this, res);
                    }
                }
            }
            break;
        }
        return res;
    }


}
