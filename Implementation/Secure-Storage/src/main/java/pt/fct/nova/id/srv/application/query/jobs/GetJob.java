package pt.fct.nova.id.srv.application.query.jobs;

import org.apache.jena.graph.Node;

public class GetJob extends Job {

    private final Node subject;
    private final Node predicate;
    private final Node object;

    public GetJob(VariablesPattern vars, Node subject, Node predicate, Node object) {
        super(vars);
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
}
