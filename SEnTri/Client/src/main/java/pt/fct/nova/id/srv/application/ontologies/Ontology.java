package pt.fct.nova.id.srv.application.ontologies;


import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.Restriction;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import java.util.Set;

public interface Ontology {

    Set<OntClass> getEquivalentClasses(Resource resource);

    Set<OntClass> getSubClasses(Resource resource);

    Restriction getRestriction(Property property);

    Set<? extends OntProperty> getSubProperties(Property property);

    Set<? extends OntProperty> getEquivalentProperties(Property property);

    Set<? extends OntProperty> getInverseOf(Property property);

    boolean isSymmetric(Property property);

    boolean isTransitive(Property property);

    Model getModel();

}
