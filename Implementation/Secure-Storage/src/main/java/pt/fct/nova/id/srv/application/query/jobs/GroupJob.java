package pt.fct.nova.id.srv.application.query.jobs;

import org.apache.jena.sparql.expr.ExprAggregator;

import java.util.List;

public class GroupJob extends Job {

    private final String prevJobID;

    private final List<ExprAggregator> aggregators;

    public GroupJob(String jobID, String prevJobID, List<ExprAggregator> aggregators) {
        super(jobID);
        this.prevJobID = prevJobID;
        this.aggregators = aggregators;
    }

    public String getPrevJobID() {
        return prevJobID;
    }

    public List<ExprAggregator> getAggregators() {
        return aggregators;
    }
}