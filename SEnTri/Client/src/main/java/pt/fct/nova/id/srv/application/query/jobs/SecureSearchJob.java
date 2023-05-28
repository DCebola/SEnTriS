package pt.fct.nova.id.srv.application.query.jobs;

import org.apache.jena.sparql.core.Var;

import java.io.Serial;

import java.util.Map;

public class SecureSearchJob extends BaseJob {
    @Serial
    private static final long serialVersionUID = 6345655033367727692L;

    private final Var[] vars;
    private final Map<Var, byte[]> searches;

    public SecureSearchJob(String jobID, Var[] vars, Map<Var, byte[]> searches) {
        super(jobID);
        this.searches = searches;
        this.vars = vars;
    }

    public Map<Var, byte[]> getSearches() {
        return searches;
    }

    public void prepareSearch(Var var, byte[] searchID) {
        searches.put(var, searchID);
    }

    public Var[] getVars() {
        return vars;
    }
}
