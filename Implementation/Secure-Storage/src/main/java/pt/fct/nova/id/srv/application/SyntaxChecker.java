package pt.fct.nova.id.srv.application;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SyntaxChecker {
    private static final Set<String> valid_syntax_map = Stream.of(
            "TURTLE", "TTL",
            "Turtle",
            "N-TRIPLES",
            "NT",
            "RDF/XML",
            "N3",
            "JSON-LD",
            "RDF/JSON",
            "RDF/JSON").collect(Collectors.toCollection(HashSet::new));

    public static boolean check(String val) {
        return valid_syntax_map.contains(val);
    }
}
