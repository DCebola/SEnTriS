package pt.fct.nova.id.srv.application.storage.idx_tables;

import org.apache.jena.sparql.core.Var;

import java.util.HashMap;
import java.util.Map;

public class MemIdxTable2 extends MemIdxTable implements IdxTable2 {

    private final Var var2;
    private final Map<String, String> idxs2;
    private final Map<String, String> rev_idxs2;


    public MemIdxTable2(Var var1, Var var2) {
        super(var1);
        this.var2 = var2;
        idxs2 = new HashMap<>();
        rev_idxs2 = new HashMap<>();
    }


    @Override
    public void insertIdx2(String tableIdx1, String tableIdx2, String idx1, String idx2) {
        super.insertIdx1(tableIdx1, idx1);
        idxs2.put(idx2, tableIdx2);
        rev_idxs2.put(tableIdx2, idx2);

    }

    @Override
    public Map<String, String> getIdxs2() {
        return idxs2;
    }

    @Override
    public Map<String, String> getRevIdxs2() {
        return rev_idxs2;
    }

    @Override
    public Var getVar2() {
        return var2;
    }
}
