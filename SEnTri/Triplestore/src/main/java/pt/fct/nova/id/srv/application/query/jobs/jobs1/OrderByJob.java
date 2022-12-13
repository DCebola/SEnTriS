package pt.fct.nova.id.srv.application.query.jobs.jobs1;

import org.apache.jena.query.SortCondition;

import java.io.Serial;
import java.util.List;

public class OrderByJob extends BaseJob1 {
    @Serial
    private static final long serialVersionUID = 5545662238582523294L;
    private final List<SortCondition> sortConditions;


    public OrderByJob(String jobID, String prevJobID, List<SortCondition> sortConditions) {
        super(jobID, prevJobID);
        this.sortConditions = sortConditions;
    }

    public List<SortCondition> getSortConditions() {
        return sortConditions;
    }
}
