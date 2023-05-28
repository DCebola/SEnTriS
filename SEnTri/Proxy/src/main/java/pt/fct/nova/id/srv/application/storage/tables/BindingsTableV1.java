package pt.fct.nova.id.srv.application.storage.tables;

import org.apache.jena.sparql.core.Var;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface BindingsTableV1 {

    void add(byte[] patternIdx, Var var, byte[] binding);

    Set<Var> getVars();

    Map<byte[], Set<byte[]>> getBindings(Var var);

    Map<byte[], byte[]> getPatternIdxs(Var var);

    Map<Var, Map<byte[], Set<byte[]>>> getBindings();

    Map<Var, Map<byte[], byte[]>> getPatternIdxs();

    List<List<byte[]>> getPatterns();

    void project(Collection<Var> vars);

    BindingsTableV1 join(BindingsTableV1 other);

    BindingsTableV1 union(BindingsTableV1 other);

    BindingsTableV1 leftOuterJoin(BindingsTableV1 other);

    BindingsTableV1 minus(BindingsTableV1 other);

}
