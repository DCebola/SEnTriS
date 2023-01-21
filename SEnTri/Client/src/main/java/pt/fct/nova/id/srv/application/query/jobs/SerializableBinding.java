package pt.fct.nova.id.srv.application.query.jobs;

import org.apache.jena.sparql.core.Var;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SerializableBinding implements Serializable {
    @Serial
    private static final long serialVersionUID = 3345654444362467694L;
    private final Map<Var, String> values;


    public SerializableBinding(Map<Var, String> values) {
        this.values = values;
    }

    public SerializableBinding() {
        this.values = new HashMap<>();
    }

    public Iterator<Var> vars() {
        return values.keySet().iterator();
    }

    public String get(Var var) {
        return values.get(var);
    }

    public Map<Var, String> getValues() {
        return values;
    }
}
