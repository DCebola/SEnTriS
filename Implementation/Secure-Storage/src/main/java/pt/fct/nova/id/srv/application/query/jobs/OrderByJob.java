package pt.fct.nova.id.srv.application.query.jobs;

import org.apache.jena.query.SortCondition;

import java.util.List;

public class OrderByJob extends Job {

    private final String prevJobID;

    private final List<SortCondition> sortConditions;


    public OrderByJob(String jobID, String prevJobID, List<SortCondition> sortConditions) {
        super(jobID);
        this.prevJobID = prevJobID;
        this.sortConditions = sortConditions;
    }

    public String getPrevJobID() {
        return prevJobID;
    }

    public List<SortCondition> getSortConditions() {
        return sortConditions;
    }
}
