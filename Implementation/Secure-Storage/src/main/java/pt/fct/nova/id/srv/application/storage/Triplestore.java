package pt.fct.nova.id.srv.application.storage;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import pt.fct.nova.id.srv.application.Index;
import static pt.fct.nova.id.srv.application.storage.RedisClient.*;

public class Triplestore {

    public void upload(Graph graph) {
        //TODO: Use bytes instead of ints for ids
        int count = 0;
        for (Triple t : graph.stream().toList()) {
            for (Index i : Index.values())
                generateIndex(i, count, t.getSubject(), t.getPredicate(), t.getObject());
            count++;
        }
    }

    private void generateIndex(Index type, int id, Node subject, Node predicate, Node object) {
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
