package pt.fct.nova.id.srv.application.query.jobs.jobs1;

import org.apache.jena.query.SortCondition;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.Job1;

import java.util.List;

public class OrderByJob extends Job1 {

    private final List<SortCondition> sortConditions;


    public OrderByJob(String jobID, String prevJobID, List<SortCondition> sortConditions) {
        super(jobID, prevJobID);
        this.sortConditions = sortConditions;
    }

    public List<SortCondition> getSortConditions() {
        return sortConditions;
    }
}
