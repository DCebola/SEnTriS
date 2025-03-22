package pt.fct.nova.id.srv.application.query.jobs;

import org.apache.jena.sparql.core.Var;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SerializableBinding<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 3345654444362467694L;
    private final Map<Var, T> values;


    public SerializableBinding(Map<Var, T> values) {
        this.values = values;
    }

    public SerializableBinding() {
        this.values = new HashMap<>();
    }

    public Iterator<Var> vars() {
        return values.keySet().iterator();
    }

    public T get(Var var) {
        return values.get(var);
    }

    public Map<Var, T> getValues() {
        return values;
    }
}
