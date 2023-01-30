package pt.fct.nova.id.srv.application.ontologies;


import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.Restriction;
import org.apache.jena.rdf.model.Model;

import java.util.Collection;

import java.util.Set;

public interface Ontology {

    void execInference(Set<Triple> schema, boolean inference);

    Collection<OntClass> getEquivalentClasses(Node node);

    Collection<OntClass> getSubClasses(Node node);

    Restriction getRestriction(Node node);

    Collection<? extends OntClass> getIntersection(Node node);

    Collection<OntClass> getIntersectionWhereClassIsOperand(Node node);

    Collection<? extends OntProperty> getSubProperties(Node node);

    Collection<? extends OntProperty> getEquivalentProperties(Node node);

    Collection<? extends OntProperty> getInverseOf(Node node);

    boolean isSymmetric(Node node);

    boolean isTransitive(Node node);

    Model getModel();

    int getTransitivityDepth();

    int getMaximumExpansionDepth();
}
