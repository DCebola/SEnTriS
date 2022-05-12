package pt.fct.nova.id.srv.application.query.bindings;

import org.apache.jena.graph.Node;

public record BindingRowItem(String var, Node node) {}
