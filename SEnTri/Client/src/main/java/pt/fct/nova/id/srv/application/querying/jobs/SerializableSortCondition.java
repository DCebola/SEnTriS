package pt.fct.nova.id.srv.application.querying.jobs;

import org.apache.jena.sparql.core.Var;

import java.io.Serial;
import java.io.Serializable;

public class SerializableSortCondition implements Serializable {
    @Serial
    private static final long serialVersionUID = 2222655234367727694L;
    private final int dir;
    private final Var var;

    public SerializableSortCondition(Var var, int dir) {
        this.var = var;
        this.dir = dir;
    }

    public SerializableSortCondition(){
        this.var = null;
        this.dir = -1;
    }

    public int getDir() {
        return dir;
    }

    public Var getVar() {
        return var;
    }
}

