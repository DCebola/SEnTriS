package pt.fct.nova.id.srv.application.storage.tables;

import org.apache.jena.sparql.core.Var;
import pt.fct.nova.id.srv.application.storage.Bytes;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface BindingsTable {

    void add(Bytes patternIdx, Var var, Bytes binding);

    Set<Var> getVars();

    Map<Bytes, Set<Bytes>> getBindings(Var var);

    Map<Bytes, Bytes> getPatternIdxs(Var var);

    Map<Var, Map<Bytes, Set<Bytes>>> getBindings();

    Map<Var, Map<Bytes, Bytes>> getPatternIdxs();

    List<List<byte[]>> getPatterns();

    void project(Collection<Var> vars);

    BindingsTable join(BindingsTable other);

    BindingsTable union(BindingsTable other);

    BindingsTable leftOuterJoin(BindingsTable other);

    BindingsTable minus(BindingsTable other);

}
