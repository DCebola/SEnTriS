package pt.fct.nova.id.srv.application.storage.tables;

import org.apache.jena.sparql.algebra.JoinType;
import org.apache.jena.sparql.core.Var;
import pt.fct.nova.id.srv.application.crypto.dgk.DGKEqKey;
import pt.fct.nova.id.srv.application.crypto.dgk.DGKEqUtils;
import pt.fct.nova.id.srv.application.crypto.dgk.HomomorphicException;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.jena.sparql.algebra.JoinType.INNER;
import static org.apache.jena.sparql.algebra.JoinType.LEFT;
import static pt.fct.nova.id.srv.application.Utils.generateID;

public class MemBindingsTableV2 implements BindingsTableV2 {

    private final Map<Var, Map<BigInteger, Set<String>>> bindings;
    private final Map<Var, Map<String, BigInteger>> patterns;

    public MemBindingsTableV2() {
        bindings = new HashMap<>();
        patterns = new HashMap<>();
    }


    public MemBindingsTableV2(Iterable<Var> vars) {
        bindings = new HashMap<>();
        patterns = new HashMap<>();
        for (Var v : vars) {
            bindings.put(v, new HashMap<>());
            patterns.put(v, new HashMap<>());
        }
    }

    public MemBindingsTableV2(Var... vars) {
        bindings = new HashMap<>();
        patterns = new HashMap<>();
        for (Var v : vars) {
            bindings.put(v, new HashMap<>());
            patterns.put(v, new HashMap<>());
        }
    }


    @Override
    public void add(String patternIdx, Var var, BigInteger binding) {
        addIRI(binding, var, patternIdx);
        addPattern(binding, var, patternIdx);
    }

    private void addPattern(BigInteger binding, Var var, String patternIdx) {
        Map<String, BigInteger> v_p_idxs = patterns.get(var);
        if (v_p_idxs == null) {
            v_p_idxs = new HashMap<>();
            v_p_idxs.put(patternIdx, binding);
            patterns.put(var, v_p_idxs);
        } else v_p_idxs.put(patternIdx, binding);
    }

    private void addIRI(BigInteger binding, Var var, String patternIdx) {
        Map<BigInteger, Set<String>> v_bindings = bindings.get(var);
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

    private void savePatternIdxs(Map<BigInteger, Set<String>> varBindings, BigInteger binding, String patternIdx) {
        Set<String> p_idxs = new HashSet<>();
        p_idxs.add(patternIdx);
        varBindings.put(binding, p_idxs);
    }

    @Override
    public Set<Var> getVars() {
        return bindings.keySet();
    }

    @Override
    public Map<BigInteger, Set<String>> getBindings(Var var) {
        return bindings.get(var);
    }

    @Override
    public Map<String, BigInteger> getPatternIdxs(Var var) {
        return patterns.get(var);
    }

    @Override
    public Map<Var, Map<BigInteger, Set<String>>> getBindings() {
        return bindings;
    }

    @Override
    public Map<Var, Map<String, BigInteger>> getPatternIdxs() {
        return patterns;
    }

    @Override
    public List<List<BigInteger>> getPatterns() {
        List<List<BigInteger>> res = new LinkedList<>();
        List<BigInteger> pattern;
        Set<Var> vars = patterns.keySet();
        Set<String> p_idxs = new HashSet<>();
        for (Var v : vars)
            p_idxs.addAll(patterns.get(v).keySet());
        BigInteger binding;
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
    public BindingsTableV2 join(BindingsTableV2 other, DGKEqKey key) throws HomomorphicException {
        Set<Var> mutual_vars = new HashSet<>(this.getVars());
        mutual_vars.retainAll(other.getVars());
        return join(key, mutual_vars, this, other, INNER);
    }


    private BindingsTableV2 join(DGKEqKey key, Set<Var> mutualVars, BindingsTableV2 left, BindingsTableV2 right, JoinType joinType) throws HomomorphicException {
        Set<Var> l_vars = new HashSet<>(left.getVars());
        Set<Var> r_vars = new HashSet<>(right.getVars());

        Set<Var> vars = new HashSet<>(l_vars);
        vars.addAll(r_vars);

        l_vars.removeAll(right.getVars());
        r_vars.removeAll(left.getVars());

        BindingsTableV2 res = new MemBindingsTableV2(vars);
        Map<BigInteger, Set<String>> l_bindings, r_bindings;
        Set<String> l_p_idxs, r_p_idxs;
        for (Var v : mutualVars) {
            l_bindings = left.getBindings(v);
            r_bindings = right.getBindings(v);
            for (Map.Entry<BigInteger, Set<String>> entry : l_bindings.entrySet()) {
                l_p_idxs = entry.getValue();
                r_p_idxs = searchVarBindings(key, r_bindings, entry.getKey());
                if (r_p_idxs != null || joinType.equals(LEFT))
                    joinPatterns(key, mutualVars, left, l_vars, l_p_idxs, right, r_vars, r_p_idxs, res, joinType);
            }
            break;
        }
        return res;
    }

    private void copyBindings(String newPattern, String oldPattern, Set<Var> vars, BindingsTableV2 source, BindingsTableV2 target) {
        for (Var v : vars) {
            BigInteger binding = source.getPatternIdxs(v).get(oldPattern);
            if (binding != null) target.add(newPattern, v, binding);
        }
    }

    private void joinPatterns(DGKEqKey key, Set<Var> mutualVars, BindingsTableV2 left, Set<Var> leftVars, Set<String> leftPatternIdxs,
                              BindingsTableV2 right, Set<Var> rightVars, Set<String> rightPatternIdxs, BindingsTableV2 res, JoinType joinType) throws HomomorphicException {
        String p;
        boolean foundMatch;
        for (String l : leftPatternIdxs) {
            foundMatch = false;
            if (rightPatternIdxs != null) {
                for (String r : rightPatternIdxs) {
                    if (equalPatterns(key, mutualVars, left, l, right, r)) {
                        foundMatch = true;
                        p = generateID();
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

    private boolean equalPatterns(DGKEqKey key, Set<Var> mutualVars, BindingsTableV2 left, String leftPattern, BindingsTableV2 right, String rightPattern) throws HomomorphicException {
        BigInteger l_binding, r_binding;
        for (Var v : mutualVars) {
            l_binding = left.getPatternIdxs(v).get(leftPattern);
            r_binding = right.getPatternIdxs(v).get(rightPattern);
            if (l_binding == null && r_binding != null)
                return false;
            else if (l_binding != null && r_binding == null)
                return false;
            else if (l_binding != null && !l_binding.equals(r_binding))
                if (!DGKEqUtils.equals(key, l_binding, r_binding))
                    return false;
        }
        return true;
    }


    @Override
    public BindingsTableV2 union(BindingsTableV2 other) {
        Set<Var> l_vars = this.getVars();
        Set<Var> r_vars = other.getVars();

        Set<Var> vars = new HashSet<>(l_vars);
        vars.addAll(r_vars);

        BindingsTableV2 res = new MemBindingsTableV2(vars);

        copyAllIRIs(l_vars, this, res);
        copyAllIRIs(r_vars, other, res);
        return res;
    }

    private void copyAllIRIs(Set<Var> vars, BindingsTableV2 source, BindingsTableV2 target) {
        for (Var v : vars) {
            for (Map.Entry<BigInteger, Set<String>> entry : source.getBindings(v).entrySet())
                for (String p : entry.getValue())
                    target.add(p, v, entry.getKey());
        }
    }

    @Override
    public BindingsTableV2 leftOuterJoin(BindingsTableV2 other, DGKEqKey key) throws HomomorphicException {
        Set<Var> mutual_vars = new HashSet<>(this.getVars());
        mutual_vars.retainAll(other.getVars());
        return join(key, mutual_vars, this, other, LEFT);
    }

    @Override
    public BindingsTableV2 minus(BindingsTableV2 other, DGKEqKey key) throws HomomorphicException {
        Set<Var> mutual_vars = new HashSet<>(this.getVars());
        mutual_vars.retainAll(other.getVars());

        Set<String> diff = new HashSet<>();
        Map<BigInteger, Set<String>> l_bindings, r_bindings;
        for (Var v : mutual_vars) {
            l_bindings = this.getBindings(v);
            r_bindings = other.getBindings(v);
            for (Map.Entry<BigInteger, Set<String>> entry : l_bindings.entrySet())
                if (searchVarBindings(key, r_bindings, entry.getKey()) == null) diff.addAll(entry.getValue());
        }

        BindingsTableV2 res = new MemBindingsTableV2(mutual_vars);
        Map<BigInteger, Set<String>> bindings;

        for (Var v : mutual_vars) {
            bindings = this.getBindings(v);
            for (Map.Entry<BigInteger, Set<String>> entry : bindings.entrySet()) {
                for (String p : entry.getValue()) {
                    if (diff.contains(p)) copyBindings(p, p, mutual_vars, this, res);
                }
            }
            break;
        }
        return res;
    }

    private Set<String> searchVarBindings(DGKEqKey key, Map<BigInteger, Set<String>> bindings, BigInteger target) throws HomomorphicException {
        BigInteger res = bindings.keySet().parallelStream()
                .filter(item -> {
                    try {
                        return DGKEqUtils.equals(key, item, target);
                    } catch (HomomorphicException e) {
                        throw new RuntimeException(e);
                    }
                })
                .findAny()
                .orElse(null);
        if (res == null)
            return null;
        else return bindings.get(res);
    }
}
