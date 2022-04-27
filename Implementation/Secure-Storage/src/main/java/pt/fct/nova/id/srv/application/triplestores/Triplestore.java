package pt.fct.nova.id.srv.application.triplestores;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

public class Triplestore {

    private static final String BLANK_URI = "_";

    public void upload(Graph graph) {
        for (Triple t : graph.stream().toList())
            uploadTriple(t.getSubject(), t.getPredicate(), t.getObject());
    }

    private void uploadTriple(Node subject, Node predicate, Node object) {
        //TODO: Upload Node + URI
        //TODO: Upload Indexes
    }
}
