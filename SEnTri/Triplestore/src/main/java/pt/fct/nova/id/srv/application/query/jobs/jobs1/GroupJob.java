package pt.fct.nova.id.srv.application.query.jobs.jobs1;

import org.apache.jena.sparql.expr.ExprAggregator;

import java.io.Serial;
import java.util.List;

public class GroupJob extends BaseJob1 {
    @Serial
    private static final long serialVersionUID = 5545662238588823294L;
    private final List<ExprAggregator> aggregators;

    public GroupJob(String jobID, String prevJobID, List<ExprAggregator> aggregators) {
        super(jobID, prevJobID);
        this.aggregators = aggregators;
    }

    public List<ExprAggregator> getAggregators() {
        return aggregators;
    }
}