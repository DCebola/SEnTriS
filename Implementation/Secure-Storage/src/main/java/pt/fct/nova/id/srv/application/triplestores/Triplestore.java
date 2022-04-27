package pt.fct.nova.id.srv.application.triplestores;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import pt.fct.nova.id.srv.application.indexes.IndexType;

public class Triplestore {

    public void upload(Graph graph) {
        //TODO: Use bytes instead of ints for ids
        for (Triple t : graph.stream().toList()) {
            for (IndexType i : IndexType.values())
                generateIndex(i, t.getSubject(), t.getPredicate(), t.getObject());
        }
    }

    private void generateIndex(IndexType type, Node subject, Node predicate, Node object) {
        switch (type) {
            case S -> {

            }
            case P -> {
            }
            case O -> {
            }
            case SP -> {
            }
            case SO -> {
            }
            case PO -> {
            }
        }
    }
}
