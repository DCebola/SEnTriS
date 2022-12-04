package pt.fct.nova.id.srv.presentation.api.dtos;

import org.apache.jena.sparql.core.Var;

import java.util.List;

public record SearchBody(List<String> trapdoors, List<Var> vars) {
}
