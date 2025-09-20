package pt.fct.nova.id.srv.application.storage.tables;

import org.apache.jena.sparql.algebra.JoinType;
import org.apache.jena.sparql.core.Var;
import pt.fct.nova.id.srv.application.storage.Bytes;

import java.util.*;

import static org.apache.jena.sparql.algebra.JoinType.INNER;
import static org.apache.jena.sparql.algebra.JoinType.LEFT;
import static pt.fct.nova.id.srv.application.Utils.generateID;

public class MemBindingsTable implements BindingsTable {

    private final Map<Var, Map<Bytes, Set<Bytes>>> bindings;
    private final Map<Var, Map<Bytes, Bytes>> patterns;

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

    public MemBindingsTable(Var... vars) {
        bindings = new HashMap<>();
        patterns = new HashMap<>();
        for (Var v : vars) {
            bindings.put(v, new HashMap<>());
            patterns.put(v, new HashMap<>());
        }
    }


    @Override
    public void add(Bytes patternIdx, Var var, Bytes binding) {
        addBinding(binding, var, patternIdx);
        addPattern(binding, var, patternIdx);
    }

    private void addPattern(Bytes binding, Var var, Bytes patternIdx) {
        Map<Bytes, Bytes> v_p_idxs = patterns.get(var);
        if (v_p_idxs == null) {
            v_p_idxs = new HashMap<>();
            v_p_idxs.put(patternIdx, binding);
            patterns.put(var, v_p_idxs);
        } else v_p_idxs.put(patternIdx, binding);
    }

    private void addBinding(Bytes binding, Var var, Bytes patternIdx) {
        Map<Bytes, Set<Bytes>> v_bindings = bindings.get(var);
        if (v_bindings == null) {
            v_bindings = new HashMap<>();
            savePatternIdx(v_bindings, binding, patternIdx);
            bindings.put(var, v_bindings);
        } else {
            Set<Bytes> p_idxs = v_bindings.get(binding);
            if (p_idxs == null) savePatternIdx(v_bindings, binding, patternIdx);
            else p_idxs.add(patternIdx);
        }
    }

    private void savePatternIdx(Map<Bytes, Set<Bytes>> varBindings, Bytes binding, Bytes patternIdx) {
        Set<Bytes> p_idxs = new HashSet<>();
        p_idxs.add(patternIdx);
        varBindings.put(binding, p_idxs);
    }

    @Override
    public Set<Var> getVars() {
        return bindings.keySet();
    }

    @Override
    public Map<Bytes, Set<Bytes>> getBindings(Var var) {
        return bindings.get(var);
    }

    @Override
    public Map<Bytes, Bytes> getPatternIdxs(Var var) {
        return patterns.get(var);
    }

    @Override
    public Map<Var, Map<Bytes, Set<Bytes>>> getBindings() {
        return bindings;
    }

    @Override
    public Map<Var, Map<Bytes, Bytes>> getPatternIdxs() {
        return patterns;
    }

    @Override
    public List<List<byte[]>> getPatterns() {
        List<List<byte[]>> res = new LinkedList<>();
        List<byte[]> pattern;
        Set<Var> vars = patterns.keySet();
        Set<Bytes> p_idxs = new HashSet<>();
        for (Var v : vars)
            p_idxs.addAll(patterns.get(v).keySet());
        Bytes binding;
        int i;
        for (Bytes p_idx : p_idxs) {
            pattern = new ArrayList<>(vars.size());
            i = 0;
            for (Var v : vars) {
                binding = patterns.get(v).get(p_idx);
                if (binding == null){
                    pattern.add(null);
                    i++;
                } else {
                    pattern.add(binding.getData());
                }
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

    private Set<Bytes> getIncompatiblePatterns(BindingsTable left, BindingsTable right, Set<Var> vars) {
        Set<Bytes> res = new HashSet<>();
        Map<Bytes, Set<Bytes>> l_bindings, r_bindings;
        for (Var v : vars) {
            l_bindings = left.getBindings(v);
            r_bindings = right.getBindings(v);
            for (Map.Entry<Bytes, Set<Bytes>> entry : l_bindings.entrySet()) {
                if (r_bindings.get(entry.getKey()) == null) res.addAll(entry.getValue());
            }
            for (Map.Entry<Bytes, Set<Bytes>> entry : r_bindings.entrySet()) {
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

        Set<Bytes> l_p_idxs, r_p_idxs;
        Map<Bytes, Set<Bytes>> l_bindings, r_bindings;

        for (Var v : mutualVars) {
            l_bindings = left.getBindings(v);
            r_bindings = right.getBindings(v);
            for (Map.Entry<Bytes, Set<Bytes>> entry : l_bindings.entrySet()) {
                l_p_idxs = entry.getValue();
                r_p_idxs = r_bindings.get(entry.getKey());
                if (r_p_idxs != null || joinType.equals(LEFT))
                    joinPatterns(mutualVars, left, l_vars, l_p_idxs, right, r_vars, r_p_idxs, res, joinType);
            }
            break;
        }
        return res;
    }

    private void copyBindings(Bytes newPattern, Bytes oldPattern, Set<Var> vars, BindingsTable source, BindingsTable target) {
        for (Var v : vars) {
            Bytes binding = source.getPatternIdxs(v).get(oldPattern);
            if (binding != null) target.add(newPattern, v, binding);
        }
    }

    private void joinPatterns(Set<Var> mutualVars, BindingsTable left, Set<Var> leftVars, Set<Bytes> leftPatternIdxs,
                              BindingsTable right, Set<Var> rightVars, Set<Bytes> rightPatternIdxs, BindingsTable res, JoinType joinType) {
        Bytes p;
        boolean foundMatch;
        for (Bytes l : leftPatternIdxs) {
            foundMatch = false;
            if (rightPatternIdxs != null) {
                for (Bytes r : rightPatternIdxs) {
                    if (equalPatterns(mutualVars, left, l, right, r)) {
                        foundMatch = true;
                        p = new Bytes(generateID());
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

    private boolean equalPatterns(Set<Var> mutualVars, BindingsTable left, Bytes leftPattern, BindingsTable right, Bytes rightPattern) {
        Bytes l_binding, r_binding;
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

        copyAllBindings(l_vars, this, res);
        copyAllBindings(r_vars, other, res);
        return res;
    }

    private void copyAllBindings(Set<Var> vars, BindingsTable source, BindingsTable target) {
        for (Var v : vars) {
            for (Map.Entry<Bytes, Set<Bytes>> entry : source.getBindings(v).entrySet()) {
                for (Bytes p : entry.getValue())
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

        Set<Bytes> diff = new HashSet<>();
        Map<Bytes, Set<Bytes>> l_bindings, r_bindings;
        for (Var v : mutual_vars) {
            l_bindings = this.getBindings(v);
            r_bindings = other.getBindings(v);
            for (Map.Entry<Bytes, Set<Bytes>> entry : l_bindings.entrySet())
                if (r_bindings.get(entry.getKey()) == null) diff.addAll(entry.getValue());
        }

        BindingsTable res = new MemBindingsTable(mutual_vars);
        Map<Bytes, Set<Bytes>> bindings;

        for (Var v : mutual_vars) {
            bindings = this.getBindings(v);
            for (Map.Entry<Bytes, Set<Bytes>> entry : bindings.entrySet()) {
                for (Bytes p : entry.getValue()) {
                    if (diff.contains(p)) copyBindings(p, p, mutual_vars, this, res);
                }
            }
            break;
        }
        return res;
    }


}
