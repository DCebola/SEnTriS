package pt.fct.nova.id.srv.application.storage.tables;

import org.apache.jena.sparql.core.Var;
import pt.fct.nova.id.srv.application.crypto.dgk.DGKEqChecker;
import pt.fct.nova.id.srv.application.crypto.dgk.HomomorphicException;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface BindingsTableV2 {

    void add(String patternIdx, Var var, BigInteger binding);

    Set<Var> getVars();

    Map<BigInteger, Set<String>> getBindings(Var var);

    Map<String, BigInteger> getPatternIdxs(Var var);

    Map<Var, Map<BigInteger, Set<String>>> getBindings();

    Map<Var, Map<String, BigInteger>> getPatternIdxs();

    List<List<BigInteger>> getPatterns();

    void project(Collection<Var> vars);

    BindingsTableV2 join(BindingsTableV2 other, DGKEqChecker eqChecker) throws HomomorphicException;

    BindingsTableV2 union(BindingsTableV2 other);

    BindingsTableV2 leftOuterJoin(BindingsTableV2 other, DGKEqChecker eqChecker) throws HomomorphicException;

    BindingsTableV2 minus(BindingsTableV2 other, DGKEqChecker eqChecker) throws HomomorphicException;

}
