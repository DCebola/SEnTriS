package pt.fct.nova.id.srv.application.storage.iri_tables;

import org.apache.jena.sparql.core.Var;

import java.util.Set;

public class MemValuesTable extends MemIRITable {

    public MemValuesTable() {
        super();
    }

    public MemValuesTable(Set<Var> vars) {
        super(vars);
    }
}
