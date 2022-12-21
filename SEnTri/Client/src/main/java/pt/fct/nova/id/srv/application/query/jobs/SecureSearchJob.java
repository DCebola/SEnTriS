package pt.fct.nova.id.srv.application.query.jobs;

import org.apache.jena.sparql.core.Var;

import java.io.Serial;

import java.util.Map;

public class SecureSearchJob extends BaseJob {
    @Serial
    private static final long serialVersionUID = 6345655033367727692L;

    private final Map<Var, String> searches;

    public SecureSearchJob(String jobID, Map<Var, String> searches) {
        super(jobID);
        this.searches = searches;
    }

    public Map<Var, String> getSearches() {
        return searches;
    }

    public void prepareSearch(Var var, String searchID) {
        searches.put(var, searchID);
    }
}
