package pt.fct.nova.id.srv.application.query.jobs;

import org.apache.jena.graph.Node;

public class GetJob extends BaseJob {

    private final VariablesPattern varsPattern;
    private final Node subject;
    private final Node predicate;
    private final Node object;

    public GetJob(String jobID, VariablesPattern vars, Node subject, Node predicate, Node object) {
        super(jobID);
        this.varsPattern = vars;
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
    }

    public Node getSubject() {
        return subject;
    }

    public Node getPredicate() {
        return predicate;
    }

    public Node getObject() {
        return object;
    }

    public VariablesPattern getVariablesPattern() {
        return varsPattern;
    }

}
