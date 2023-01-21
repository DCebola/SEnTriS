package pt.fct.nova.id.srv.application.ontologies;


import org.apache.jena.graph.Node;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.Restriction;
import org.apache.jena.rdf.model.Model;

import java.util.Set;

public interface Ontology {

    Set<OntClass> getEquivalentClasses(Node node);

    Set<OntClass> getSubClasses(Node node);

    Restriction getRestriction(Node node);

    Set<OntClass> getIntersection(Node node);

    Set<? extends OntProperty> getSubProperties(Node node);

    Set<? extends OntProperty> getEquivalentProperties(Node node);

    Set<? extends OntProperty> getInverseOf(Node node);

    Set<OntClass> getRange(Node node);

    boolean isSymmetric(Node node);

    boolean isTransitive(Node node);

    Model getModel();

    int getTransitivityDepth();

    int getMaximumExpansionDepth();
}
