package pt.fct.nova.id.srv.application.storage.tables;

import org.apache.jena.sparql.algebra.JoinType;
import org.apache.jena.sparql.core.Var;
import pt.fct.nova.id.srv.application.crypto.dgk.DGKEqKey;
import pt.fct.nova.id.srv.application.crypto.dgk.DGKEqUtils;
import pt.fct.nova.id.srv.application.crypto.dgk.HomomorphicException;
import pt.fct.nova.id.srv.application.storage.Bytes;

import java.math.BigInteger;
import java.util.*;

import static org.apache.jena.sparql.algebra.JoinType.INNER;
import static org.apache.jena.sparql.algebra.JoinType.LEFT;
import static pt.fct.nova.id.srv.application.Utils.generateID;

public class MemBindingsTableV2 implements BindingsTableV2 {

    private final Map<Var, Map<BigInteger, Set<Bytes>>> bindings;
    private final Map<Var, Map<Bytes, BigInteger>> patterns;

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
    public void add(Bytes patternIdx, Var var, BigInteger binding) {
        addBinding(binding, var, patternIdx);
        addPattern(binding, var, patternIdx);
    }

    private void addPattern(BigInteger binding, Var var, Bytes patternIdx) {
        Map<Bytes, BigInteger> v_p_idxs = patterns.get(var);
        if (v_p_idxs == null) {
            v_p_idxs = new HashMap<>();
            v_p_idxs.put(patternIdx, binding);
            patterns.put(var, v_p_idxs);
        } else v_p_idxs.put(patternIdx, binding);
    }

    private void addBinding(BigInteger binding, Var var, Bytes patternIdx) {
        Map<BigInteger, Set<Bytes>> v_bindings = bindings.get(var);
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

    private void savePatternIdx(Map<BigInteger, Set<Bytes>> varBindings, BigInteger binding, Bytes patternIdx) {
        Set<Bytes> p_idxs = new HashSet<>();
        p_idxs.add(patternIdx);
        varBindings.put(binding, p_idxs);
    }

    @Override
    public Set<Var> getVars() {
        return bindings.keySet();
    }

    @Override
    public Map<BigInteger, Set<Bytes>> getBindings(Var var) {
        return bindings.get(var);
    }

    @Override
    public Map<Bytes, BigInteger> getPatternIdxs(Var var) {
        return patterns.get(var);
    }

    @Override
    public Map<Var, Map<BigInteger, Set<Bytes>>> getBindings() {
        return bindings;
    }

    @Override
    public Map<Var, Map<Bytes, BigInteger>> getPatternIdxs() {
        return patterns;
    }

    @Override
    public List<List<BigInteger>> getPatterns() {
        List<List<BigInteger>> res = new LinkedList<>();
        List<BigInteger> pattern;
        Set<Var> vars = patterns.keySet();
        Set<Bytes> p_idxs = new HashSet<>();
        for (Var v : vars)
            p_idxs.addAll(patterns.get(v).keySet());
        BigInteger binding;
        int i;
        for (Bytes p_idx : p_idxs) {
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
        Map<BigInteger, Set<Bytes>> l_bindings, r_bindings;
        Set<Bytes> l_p_idxs, r_p_idxs;
        for (Var v : mutualVars) {
            l_bindings = left.getBindings(v);
            r_bindings = right.getBindings(v);
            for (Map.Entry<BigInteger, Set<Bytes>> entry : l_bindings.entrySet()) {
                l_p_idxs = entry.getValue();
                r_p_idxs = searchVarBindings(key, r_bindings, entry.getKey());
                if (r_p_idxs != null || joinType.equals(LEFT))
                    joinPatterns(key, mutualVars, left, l_vars, l_p_idxs, right, r_vars, r_p_idxs, res, joinType);
            }
            break;
        }
        return res;
    }

    private void copyBindings(Bytes newPattern, Bytes oldPattern, Set<Var> vars, BindingsTableV2 source, BindingsTableV2 target) {
        for (Var v : vars) {
            BigInteger binding = source.getPatternIdxs(v).get(oldPattern);
            if (binding != null) target.add(newPattern, v, binding);
        }
    }

    private void joinPatterns(DGKEqKey key, Set<Var> mutualVars, BindingsTableV2 left, Set<Var> leftVars, Set<Bytes> leftPatternIdxs,
                              BindingsTableV2 right, Set<Var> rightVars, Set<Bytes> rightPatternIdxs, BindingsTableV2 res, JoinType joinType) throws HomomorphicException {
        Bytes p;
        boolean foundMatch;
        for (Bytes l : leftPatternIdxs) {
            foundMatch = false;
            if (rightPatternIdxs != null) {
                for (Bytes r : rightPatternIdxs) {
                    if (equalPatterns(key, mutualVars, left, l, right, r)) {
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

    private boolean equalPatterns(DGKEqKey key, Set<Var> mutualVars, BindingsTableV2 left, Bytes leftPattern, BindingsTableV2 right, Bytes rightPattern) throws HomomorphicException {
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

        copyAllBindings(l_vars, this, res);
        copyAllBindings(r_vars, other, res);
        return res;
    }

    private void copyAllBindings(Set<Var> vars, BindingsTableV2 source, BindingsTableV2 target) {
        for (Var v : vars) {
            for (Map.Entry<BigInteger, Set<Bytes>> entry : source.getBindings(v).entrySet())
                for (Bytes p : entry.getValue())
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

        Set<Bytes> diff = new HashSet<>();
        Map<BigInteger, Set<Bytes>> l_bindings, r_bindings;
        for (Var v : mutual_vars) {
            l_bindings = this.getBindings(v);
            r_bindings = other.getBindings(v);
            for (Map.Entry<BigInteger, Set<Bytes>> entry : l_bindings.entrySet())
                if (searchVarBindings(key, r_bindings, entry.getKey()) == null) diff.addAll(entry.getValue());
        }

        BindingsTableV2 res = new MemBindingsTableV2(mutual_vars);
        Map<BigInteger, Set<Bytes>> bindings;

        for (Var v : mutual_vars) {
            bindings = this.getBindings(v);
            for (Map.Entry<BigInteger, Set<Bytes>> entry : bindings.entrySet()) {
                for (Bytes p : entry.getValue()) {
                    if (diff.contains(p)) copyBindings(p, p, mutual_vars, this, res);
                }
            }
            break;
        }
        return res;
    }

    private Set<Bytes> searchVarBindings(DGKEqKey key, Map<BigInteger, Set<Bytes>> bindings, BigInteger target) throws HomomorphicException {
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
