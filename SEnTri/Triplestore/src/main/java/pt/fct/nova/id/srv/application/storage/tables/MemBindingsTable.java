package pt.fct.nova.id.srv.application.storage.tables;

import org.apache.jena.sparql.algebra.JoinType;
import org.apache.jena.sparql.core.Var;

import java.util.*;

import static org.apache.jena.sparql.algebra.JoinType.INNER;
import static org.apache.jena.sparql.algebra.JoinType.LEFT;
import static pt.fct.nova.id.srv.application.Utils.generateB64ID;

public class MemBindingsTable implements BindingsTable {

    private final Map<Var, Map<String, Set<String>>> bindings;
    private final Map<Var, Map<String, String>> patterns;

    public MemBindingsTable() {
        bindings = new HashMap<>();
        patterns = new HashMap<>();
    }

    public MemBindingsTable(Iterable<Var> vars) {
        bindings = new HashMap<>();
        patterns = new HashMap<>();
        for (Var v : vars) {
            bindings.put(v, new HashMap<>());
            patterns.put(v, new HashMap<>());
        }
    }

    public MemBindingsTable(Var... var) {
        bindings = new HashMap<>();
        patterns = new HashMap<>();
        for (Var v : var) {
            bindings.put(v, new HashMap<>());
            patterns.put(v, new HashMap<>());
        }
    }


    @Override
    public void add(String patternIdx, Var var, String binding) {
        addIRI(binding, var, patternIdx);
        addPattern(binding, var, patternIdx);
    }

    private void addPattern(String binding, Var var, String patternIdx) {
        Map<String, String> v_p_idxs = patterns.get(var);
        if (v_p_idxs == null) {
            v_p_idxs = new HashMap<>();
            v_p_idxs.put(patternIdx, binding);
            patterns.put(var, v_p_idxs);
        } else v_p_idxs.put(patternIdx, binding);
    }

    private void addIRI(String binding, Var var, String patternIdx) {
        Map<String, Set<String>> v_bindings = bindings.get(var);
        if (v_bindings == null) {
            v_bindings = new HashMap<>();
            savePatternIdxs(v_bindings, binding, patternIdx);
            bindings.put(var, v_bindings);
        } else {
            Set<String> p_idxs = v_bindings.get(binding);
            if (p_idxs == null) savePatternIdxs(v_bindings, binding, patternIdx);
            else p_idxs.add(patternIdx);
        }
    }

    private void savePatternIdxs(Map<String, Set<String>> varBindings, String binding, String patternIdx) {
        Set<String> p_idxs = new HashSet<>();
        p_idxs.add(patternIdx);
        varBindings.put(binding, p_idxs);
    }

    @Override
    public Set<Var> getVars() {
        return bindings.keySet();
    }

    @Override
    public Map<String, Set<String>> getBindings(Var var) {
        return bindings.get(var);
    }

    @Override
    public Map<String, String> getPatternIdxs(Var var) {
        return patterns.get(var);
    }

    @Override
    public Map<Var, Map<String, Set<String>>> getBindings() {
        return bindings;
    }

    @Override
    public Map<Var, Map<String, String>> getPatternIdxs() {
        return patterns;
    }

    @Override
    public List<List<String>> getPatterns() {
        List<List<String>> res = new LinkedList<>();
        List<String> pattern;
        Set<Var> vars = patterns.keySet();
        Set<String> p_idxs = new HashSet<>();
        for (Var v : vars)
            p_idxs.addAll(patterns.get(v).keySet());
        String binding;
        int i;
        for (String p_idx : p_idxs) {
            pattern = new ArrayList<>(vars.size());
            i = 0;
            for (Var v : vars) {
                binding = patterns.get(v).get(p_idx);
                pattern.add(binding);
                if (binding == null) i++;
            }
            if (i < vars.size()) res.add(pattern);
        }
        return res;
    }

    @Override
    public void project(Collection<Var> vars) {
        bindings.keySet().retainAll(vars);
        patterns.keySet().retainAll(vars);
    }

    @Override
    public BindingsTable join(BindingsTable other) {
        Set<Var> mutual_vars = new HashSet<>(this.getVars());
        mutual_vars.retainAll(other.getVars());
        return join(mutual_vars, this, other, INNER);
    }

    private Set<String> getIncompatiblePatterns(BindingsTable left, BindingsTable right, Set<Var> vars) {
        Set<String> res = new HashSet<>();
        Map<String, Set<String>> l_bindings, r_bindings;
        for (Var v : vars) {
            l_bindings = left.getBindings(v);
            r_bindings = right.getBindings(v);
            for (Map.Entry<String, Set<String>> entry : l_bindings.entrySet()) {
                if (r_bindings.get(entry.getKey()) == null) res.addAll(entry.getValue());
            }
            for (Map.Entry<String, Set<String>> entry : r_bindings.entrySet()) {
                if (l_bindings.get(entry.getKey()) == null) res.addAll(entry.getValue());
            }
        }
        return res;
    }


    private BindingsTable join(Set<Var> mutualVars, BindingsTable left, BindingsTable right, JoinType joinType) {
        Set<Var> l_vars = new HashSet<>(left.getVars());
        Set<Var> r_vars = new HashSet<>(right.getVars());

        Set<Var> vars = new HashSet<>(l_vars);
        vars.addAll(r_vars);

        l_vars.removeAll(right.getVars());
        r_vars.removeAll(left.getVars());

        BindingsTable res = new MemBindingsTable(vars);

        Set<String> l_p_idxs, r_p_idxs;
        Map<String, Set<String>> l_bindings, r_bindings;

        for (Var v : mutualVars) {
            l_bindings = left.getBindings(v);
            r_bindings = right.getBindings(v);
            for (Map.Entry<String, Set<String>> entry : l_bindings.entrySet()) {
                l_p_idxs = entry.getValue();
                r_p_idxs = r_bindings.get(entry.getKey());
                if (r_p_idxs != null || joinType.equals(LEFT))
                    joinPatterns(mutualVars, left, l_vars, l_p_idxs, right, r_vars, r_p_idxs, res, joinType);
            }
            break;
        }
        return res;
    }

    private void copyBindings(String newPattern, String oldPattern, Set<Var> vars, BindingsTable source, BindingsTable target) {
        for (Var v : vars) {
            String binding = source.getPatternIdxs(v).get(oldPattern);
            if (binding != null) target.add(newPattern, v, binding);
        }
    }

    private void joinPatterns(Set<Var> mutualVars, BindingsTable left, Set<Var> leftVars, Set<String> leftPatternIdxs,
                              BindingsTable right, Set<Var> rightVars, Set<String> rightPatternIdxs, BindingsTable res, JoinType joinType) {
        String p;
        boolean foundMatch;
        for (String l : leftPatternIdxs) {
            foundMatch = false;
            if (rightPatternIdxs != null) {
                for (String r : rightPatternIdxs) {
                    if (equalPatterns(mutualVars, left, l, right, r)) {
                        foundMatch = true;
                        p = generateB64ID();
                        copyBindings(p, l, mutualVars, left, res);
                        copyBindings(p, l, leftVars, left, res);
                        copyBindings(p, r, rightVars, right, res);
                    }
                }
            }
            if (!foundMatch && joinType.equals(LEFT)) {
                copyBindings(l, l, mutualVars, left, res);
                copyBindings(l, l, leftVars, left, res);
            }
        }
    }

    private boolean equalPatterns(Set<Var> mutualVars, BindingsTable left, String leftPattern, BindingsTable right, String rightPattern) {
        String l_binding, r_binding;
        for (Var v : mutualVars) {
            l_binding = left.getPatternIdxs(v).get(leftPattern);
            r_binding = right.getPatternIdxs(v).get(rightPattern);
            if (l_binding == null && r_binding != null)
                return false;
            else if (l_binding != null && r_binding == null)
                return false;
            else if (l_binding != null && !l_binding.equals(r_binding))
                return false;
        }
        return true;
    }


    @Override
    public BindingsTable union(BindingsTable other) {
        Set<Var> l_vars = this.getVars();
        Set<Var> r_vars = other.getVars();

        Set<Var> vars = new HashSet<>(l_vars);
        vars.addAll(r_vars);

        BindingsTable res = new MemBindingsTable(vars);

        copyAllIRIs(l_vars, this, res);
        copyAllIRIs(r_vars, other, res);
        return res;
    }

    private void copyAllIRIs(Set<Var> vars, BindingsTable source, BindingsTable target) {
        for (Var v : vars) {
            for (Map.Entry<String, Set<String>> entry : source.getBindings(v).entrySet()) {
                for (String p : entry.getValue())
                    target.add(p, v, entry.getKey());
            }
        }
    }

    @Override
    public BindingsTable leftOuterJoin(BindingsTable other) {
        Set<Var> mutual_vars = new HashSet<>(this.getVars());
        mutual_vars.retainAll(other.getVars());
        return join(mutual_vars, this, other, LEFT);
    }

    @Override
    public BindingsTable minus(BindingsTable other) {
        Set<Var> mutual_vars = new HashSet<>(this.getVars());
        mutual_vars.retainAll(other.getVars());

        Set<String> diff = getIncompatiblePatterns(this, other, mutual_vars);

        BindingsTable res = new MemBindingsTable(mutual_vars);
        Map<String, Set<String>> bindings;

        for (Var v : mutual_vars) {
            bindings = this.getBindings(v);
            for (Map.Entry<String, Set<String>> entry : bindings.entrySet()) {
                for (String p : entry.getValue()) {
                    if (diff.contains(p)) copyBindings(p, p, mutual_vars, this, res);
                }
            }
            break;
        }
        return res;
    }


}
