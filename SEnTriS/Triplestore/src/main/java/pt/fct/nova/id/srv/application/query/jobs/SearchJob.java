package pt.fct.nova.id.srv.application.query.jobs;

import org.apache.jena.graph.Node;

import java.io.Serial;

public class SearchJob extends BaseJob {
    @Serial
    private static final long serialVersionUID = 6345655033367727691L;
    private final VariablesPattern varsPattern;
    private final Node subject;
    private final Node predicate;
    private final Node object;


    public SearchJob(String jobID, VariablesPattern vars, Node subject, Node predicate, Node object) {
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
