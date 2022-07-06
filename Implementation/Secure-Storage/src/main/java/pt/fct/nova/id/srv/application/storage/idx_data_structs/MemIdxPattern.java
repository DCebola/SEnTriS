package pt.fct.nova.id.srv.application.storage.idx_data_structs;

import org.apache.jena.sparql.core.Var;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MemIdxPattern implements IdxPattern {

    private final List<Var> vars;
    private final List<String> idxs;

    public MemIdxPattern(List<Var> vars, List<String> idxs) {
        this.vars = vars;
        this.idxs = idxs;
    }

    public MemIdxPattern(int numVars) {
        this.vars = new ArrayList<>(numVars);
        this.idxs = new LinkedList<>();
    }

    @Override
    public List<Var> getVars() {
        return vars;
    }

    @Override
    public List<String> getIdxs() {
        return idxs;
    }

    @Override
    public void addIdx(String idx) {
        idxs.add(idx);
    }

    @Override
    public void addVar(Var var) {
        vars.add(var);
    }
}
