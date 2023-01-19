package pt.fct.nova.id.srv.application.querying.jobs.jobs1;

import pt.fct.nova.id.srv.application.querying.jobs.SerializableSortCondition;

import java.io.Serial;
import java.util.List;

public class OrderByJob extends BaseJob1 {
    @Serial
    private static final long serialVersionUID = 5545662238582523294L;
    private final List<SerializableSortCondition> sortConditions;


    public OrderByJob(String jobID, String prevJobID, List<SerializableSortCondition> sortConditions) {
        super(jobID, prevJobID);
        this.sortConditions = sortConditions;
    }

    public List<SerializableSortCondition> getSortConditions() {
        return sortConditions;
    }
}
