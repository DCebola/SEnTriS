package pt.fct.nova.id.srv.application.query.execution.exceptions;

import org.apache.jena.graph.Node;

public class ValuesNodeException extends SPARQLExecutionException {
    public ValuesNodeException(String jobInstance, String job, Node node) {
        super(String.format("[%s , %s] - Invalid node: %s", jobInstance, job, node));
    }
}
