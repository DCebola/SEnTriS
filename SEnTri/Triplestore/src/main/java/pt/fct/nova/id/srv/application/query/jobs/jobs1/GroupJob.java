package pt.fct.nova.id.srv.application.query.jobs.jobs1;

import org.apache.jena.sparql.expr.ExprAggregator;

import java.util.List;

public class GroupJob extends BaseJob1 {

    private final List<ExprAggregator> aggregators;

    public GroupJob(String jobID, String prevJobID, List<ExprAggregator> aggregators) {
        super(jobID, prevJobID);
        this.aggregators = aggregators;
    }

    public List<ExprAggregator> getAggregators() {
        return aggregators;
    }
}