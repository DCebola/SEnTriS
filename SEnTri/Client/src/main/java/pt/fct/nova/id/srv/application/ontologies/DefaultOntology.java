package pt.fct.nova.id.srv.application.ontologies;

import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Triple;
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.PrintUtil;
import org.apache.jena.util.iterator.ExtendedIterator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.apache.jena.ontology.OntModelSpec.OWL_MEM;
import static org.apache.jena.ontology.OntModelSpec.OWL_MEM_TRANS_INF;

public class DefaultOntology implements Ontology {
    Map<Resource, Set<OntClass>> subClasses = new HashMap<>();
    Map<Resource, Set<OntClass>> equivalentClasses = new HashMap<>();
    Map<Property, Restriction> classRestrictions = new HashMap<>();
    Map<Property, Set<? extends OntProperty>> subProperties = new HashMap<>();
    Map<Property, Set<? extends OntProperty>> equivalentProperties = new HashMap<>();
    Map<Property, Set<? extends OntProperty>> inverseProperties = new HashMap<>();
    Set<Property> symmetricProperties = new HashSet<>();
    Set<Property> transitiveProperties = new HashSet<>();
    OntModel ontology;
    OntModelSpec spec;
    String triplestoreID;

    public DefaultOntology(String triplestoreID, Set<Triple> schema, boolean inference) {
        this.spec = OWL_MEM_TRANS_INF;
        this.triplestoreID = triplestoreID;
        OntModel tbox = ModelFactory.createOntologyModel(OWL_MEM);
        GraphUtil.add(tbox.getGraph(), schema.iterator());
        if (inference) {
            this.ontology = ModelFactory.createOntologyModel(spec, tbox);
            execClassInference();
            execPropertyInference();
        } else
            ontology = tbox;
    }

    private void execClassInference() {
        Set<OntClass> s;
        OntClass currentClass;
        for (ExtendedIterator<OntClass> it = ontology.listClasses(); it.hasNext(); ) {
            currentClass = it.next();
            if (currentClass.isResource()) {
                System.out.println(" - " + PrintUtil.print(currentClass));
                s = currentClass.listSubClasses().toSet();
                for (OntClass c2 : s)
                    System.out.println(" s- " + PrintUtil.print(c2));
                subClasses.put(currentClass.asResource(), s);
                s = currentClass.listEquivalentClasses().toSet();
                for (OntClass c2 : s)
                    System.out.println(" eq- " + PrintUtil.print(c2));
                equivalentClasses.put(currentClass.asResource(), s);
            } else {
                System.out.println(" n- " + PrintUtil.print(currentClass));
            }
        }
        Restriction currentRestriction;
        for (ExtendedIterator<Restriction> it = ontology.listRestrictions(); it.hasNext(); ) {
            currentRestriction = it.next();
            if (currentRestriction.isSomeValuesFromRestriction()) {
                System.out.println(" r- " + PrintUtil.print(currentRestriction) +
                        " | " + PrintUtil.print(currentRestriction.getOnProperty())
                        + " | " + PrintUtil.print(currentRestriction.asSomeValuesFromRestriction().getSomeValuesFrom()));
                classRestrictions.put(currentRestriction.getOnProperty(), currentRestriction);
            } else if (currentRestriction.isHasValueRestriction()) {
                System.out.println(" r- " + PrintUtil.print(currentRestriction) +
                        " | " + PrintUtil.print(currentRestriction.getOnProperty())
                        + " | " + PrintUtil.print(currentRestriction.asHasValueRestriction().getHasValue()));
                classRestrictions.put(currentRestriction.getOnProperty(), currentRestriction);
            }
        }
    }

    private void execPropertyInference() {
        Set<? extends OntProperty> s;
        for (OntProperty p : ontology.listOntProperties().toSet()) {
            if (p.isProperty()) {
                extractPropertyInfo(p);
                s = p.listSubProperties().toSet();
                for (OntProperty p2 : s) {
                    System.out.println(" su++ " + PrintUtil.print(p2));
                }
                subProperties.put(p.asProperty(), s);
                s = p.listEquivalentProperties().toSet();
                for (OntProperty p2 : s) {
                    System.out.println(" eq++ " + PrintUtil.print(p2));
                }
                equivalentProperties.put(p.asProperty(), s);
            } else {
                System.out.println(" n+ " + PrintUtil.print(p));
            }
        }
    }

    private void extractPropertyInfo(OntProperty p) {
        if (p.isInverseFunctionalProperty()) {
            System.out.println(" i+ " + PrintUtil.print(p));
            inverseProperties.put(p.asProperty(), p.listInverseOf().toSet());
        } else if (p.isSymmetricProperty()) {
            System.out.println(" sy+ " + PrintUtil.print(p));
            symmetricProperties.add(p.asProperty());
        } else if (p.isTransitiveProperty()) {
            System.out.println(" t+ " + PrintUtil.print(p));
            transitiveProperties.add(p.asProperty());
        } else {
            System.out.println(" + " + PrintUtil.print(p));
        }
    }

    @Override
    public Set<OntClass> getEquivalentClasses(Resource resource) {
        Set<OntClass> classes = equivalentClasses.get(resource);
        return classes == null ? new HashSet<>() : classes;
    }

    @Override
    public Set<OntClass> getSubClasses(Resource resource) {
        Set<OntClass> classes = subClasses.get(resource);
        return classes == null ? new HashSet<>() : classes;
    }

    @Override
    public Restriction getRestriction(Property property) {
        return classRestrictions.get(property);
    }

    @Override
    public Set<? extends OntProperty> getSubProperties(Property property) {
        Set<? extends OntProperty> properties = subProperties.get(property);
        return properties == null ? new HashSet<>() : properties;
    }

    @Override
    public Set<? extends OntProperty> getEquivalentProperties(Property property) {
        Set<? extends OntProperty> properties = equivalentProperties.get(property);
        return properties == null ? new HashSet<>() : properties;
    }

    @Override
    public Set<? extends OntProperty> getInverseOf(Property property) {
        Set<? extends OntProperty> properties = inverseProperties.get(property);
        return properties == null ? new HashSet<>() : properties;
    }


    @Override
    public boolean isSymmetric(Property property) {
        return symmetricProperties.contains(property);
    }

    @Override
    public boolean isTransitive(Property property) {
        return transitiveProperties.contains(property);
    }

    @Override
    public Model getModel() {
        return ontology.difference(ModelFactory.createOntologyModel(OWL_MEM));
    }

}
