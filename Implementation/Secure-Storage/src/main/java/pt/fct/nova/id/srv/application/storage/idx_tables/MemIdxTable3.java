package pt.fct.nova.id.srv.application.storage.idx_tables;

import org.apache.jena.sparql.core.Var;

import java.util.HashMap;
import java.util.Map;

public class MemIdxTable3 extends MemIdxTable2 implements IdxTable3 {

    private final Var var3;
    private final Map<String, String> idxs3;
    private final Map<String, String> rev_idxs3;


    public MemIdxTable3(Var var1, Var var2, Var var3) {
        super(var1, var2);
        this.var3 = var2;
        idxs3 = new HashMap<>();
        rev_idxs3 = new HashMap<>();
    }

    @Override
    public void insertIdx3(String tableIdx1, String tableIdx2, String tableIdx3, String idx1, String idx2, String idx3) {
        super.insertIdx2(tableIdx1, tableIdx2, idx1, idx2);
        idxs3.put(idx2, tableIdx2);
        rev_idxs3.put(tableIdx2, idx2);
    }

    @Override
    public Map<String, String> getIdxs3() {
        return idxs3;
    }

    @Override
    public Map<String, String> getRevIdxs3() {
        return rev_idxs3;
    }

    @Override
    public Var getVar3() {
        return var3;
    }

}
