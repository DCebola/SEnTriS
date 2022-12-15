package pt.fct.nova.id.srv.application.storage.iri_tables;

import org.apache.jena.sparql.algebra.JoinType;
import org.apache.jena.sparql.core.Var;

import java.security.SecureRandom;
import java.util.*;

import static org.apache.jena.sparql.algebra.JoinType.INNER;
import static org.apache.jena.sparql.algebra.JoinType.LEFT;
import static pt.fct.nova.id.srv.application.Utils.generateID;

public class MemIRITable implements IRITable {

    private final Map<Var, Map<String, Set<String>>> iris;
    private final Map<Var, Map<String, String>> patterns;

    public MemIRITable() {
        iris = new HashMap<>();
        patterns = new HashMap<>();
    }

    public MemIRITable(Iterable<Var> vars) {
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
        } else v_p_idxs.put(patternIdx, iri);
    }

    private void addIRI(String iri, Var var, String patternIdx) {
        Map<String, Set<String>> v_iris = iris.get(var);
        if (v_iris == null) {
            v_iris = new HashMap<>();
            savePatternIdxs(v_iris, iri, patternIdx);
            iris.put(var, v_iris);
        } else {
            Set<String> p_idxs = v_iris.get(iri);
            if (p_idxs == null) savePatternIdxs(v_iris, iri, patternIdx);
            else p_idxs.add(patternIdx);
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
    public List<List<String>> getPatterns() {
        List<List<String>> res = new LinkedList<>();
        List<String> pattern;
        Set<Var> vars = patterns.keySet();
        Set<String> p_idxs = new HashSet<>();
        for (Var v : vars)
            p_idxs.addAll(patterns.get(v).keySet());
        String iri;
        int i;
        for (String p_idx : p_idxs) {
            pattern = new ArrayList<>(vars.size());
            i = 0;
            for (Var v : vars) {
                iri = patterns.get(v).get(p_idx);
                pattern.add(iri);
                if (iri == null) i++;
            }
            if (i < vars.size()) res.add(pattern);
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
        return join(mutual_vars, this, other, INNER);
    }

    private Set<String> getIncompatiblePatterns(IRITable left, IRITable right, Set<Var> vars) {
        Set<String> res = new HashSet<>();
        Map<String, Set<String>> left_iris, right_iris;
        for (Var v : vars) {
            left_iris = left.getIRIs(v);
            right_iris = right.getIRIs(v);
            for (Map.Entry<String, Set<String>> entry : left_iris.entrySet()) {
                if (right_iris.get(entry.getKey()) == null) res.addAll(entry.getValue());
            }
            for (Map.Entry<String, Set<String>> entry : right_iris.entrySet()) {
                if (left_iris.get(entry.getKey()) == null) res.addAll(entry.getValue());
            }
        }
        return res;
    }


    private IRITable join(Set<Var> mutualVars, IRITable left, IRITable right, JoinType joinType) {
        Set<Var> l_vars = new HashSet<>(left.getVars());
        Set<Var> r_vars = new HashSet<>(right.getVars());

        Set<Var> vars = new HashSet<>(l_vars);
        vars.addAll(r_vars);

        l_vars.removeAll(right.getVars());
        r_vars.removeAll(left.getVars());

        IRITable res;
        if (left instanceof MemValuesTable && right instanceof MemValuesTable)
            res = new MemValuesTable(vars);
        else
            res = new MemIRITable(vars);

        Set<String> l_p_idxs, r_p_idxs;
        Map<String, Set<String>> iris_map, iris_map2;


        for (Var v : mutualVars) {
            iris_map = left.getIRIs(v);
            iris_map2 = right.getIRIs(v);
            for (Map.Entry<String, Set<String>> entry : iris_map.entrySet()) {
                l_p_idxs = entry.getValue();
                r_p_idxs = iris_map2.get(entry.getKey());
                if (r_p_idxs != null || joinType.equals(LEFT))
                    joinPatterns(mutualVars, left, l_vars, l_p_idxs, right, r_vars, r_p_idxs, res, joinType);
            }
            break;
        }
        return res;
    }

    private void copyIRIs(String newPattern, String oldPattern, Set<Var> vars, IRITable source, IRITable target) {
        for (Var v : vars) {
            String iri = source.getPatternIdxs(v).get(oldPattern);
            if (iri != null) target.add(newPattern, v, iri);
        }
    }

    private void joinPatterns(Set<Var> mutualVars, IRITable left, Set<Var> leftVars, Set<String> leftPatternIdxs, IRITable right, Set<Var> rightVars, Set<String> rightPatternIdxs, IRITable res, JoinType joinType) {
        String p;
        boolean foundMatch;
        for (String l : leftPatternIdxs) {
            foundMatch = false;
            if (rightPatternIdxs != null) {
                for (String r : rightPatternIdxs) {
                    if (equalPatterns(mutualVars, left, l, right, r)) {
                        foundMatch = true;
                        p = generateID();
                        copyIRIs(p, l, mutualVars, left, res);
                        copyIRIs(p, l, leftVars, left, res);
                        copyIRIs(p, r, rightVars, right, res);
                    }
                }
            }
            if (!foundMatch && joinType.equals(LEFT)) {
                copyIRIs(l, l, mutualVars, left, res);
                copyIRIs(l, l, leftVars, left, res);
            }
        }
    }

    private boolean equalPatterns(Set<Var> mutualVars, IRITable left, String leftPattern, IRITable right, String rightPattern) {
        String leftIRI, rightIRI;
        for (Var v : mutualVars) {
            leftIRI = left.getPatternIdxs(v).get(leftPattern);
            rightIRI = right.getPatternIdxs(v).get(rightPattern);

            if (left instanceof MemValuesTable || right instanceof MemValuesTable) {
                if (leftIRI != null && rightIRI != null && !leftIRI.equals(rightIRI)) {
                    return false;
                }
            } else {
                if (leftIRI == null && rightIRI != null)
                    return false;
                else if (leftIRI != null && rightIRI == null)
                    return false;
                else if (leftIRI != null && !leftIRI.equals(rightIRI))
                    return false;
            }
        }
        return true;
    }


    @Override
    public IRITable union(IRITable other) {
        Set<Var> l_vars = this.getVars();
        Set<Var> r_vars = other.getVars();

        Set<Var> vars = new HashSet<>(l_vars);
        vars.addAll(r_vars);

        IRITable res = new MemIRITable(vars);

        copyAllIRIs(l_vars, this, res);
        copyAllIRIs(r_vars, other, res);
        return res;
    }

    private void copyAllIRIs(Set<Var> vars, IRITable source, IRITable target) {
        for (Var v : vars) {
            for (Map.Entry<String, Set<String>> entry : source.getIRIs(v).entrySet()) {
                for (String p : entry.getValue())
                    target.add(p, v, entry.getKey());
            }
        }
    }

    @Override
    public IRITable leftOuterJoin(IRITable other) {
        Set<Var> mutual_vars = new HashSet<>(this.getVars());
        mutual_vars.retainAll(other.getVars());
        return join(mutual_vars, this, other, LEFT);
    }

    @Override
    public IRITable minus(IRITable other) {
        Set<Var> mutual_vars = new HashSet<>(this.getVars());
        mutual_vars.retainAll(other.getVars());

        Set<String> diff = getIncompatiblePatterns(this, other, mutual_vars);

        IRITable res = new MemIRITable(mutual_vars);
        Map<String, Set<String>> iris_map;

        for (Var v : mutual_vars) {
            iris_map = this.getIRIs(v);
            for (Map.Entry<String, Set<String>> entry : iris_map.entrySet()) {
                for (String p : entry.getValue()) {
                    if (diff.contains(p)) copyIRIs(p, p, mutual_vars, this, res);
                }
            }
            break;
        }
        return res;
    }


}
