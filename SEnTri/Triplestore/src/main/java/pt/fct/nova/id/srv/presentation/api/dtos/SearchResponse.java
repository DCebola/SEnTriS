package pt.fct.nova.id.srv.presentation.api.dtos;

import org.apache.jena.sparql.core.Var;

import java.util.ArrayList;
import java.util.List;

public record SearchResponse(ArrayList<Var> vars, List<List<String>> patterns) {
}
