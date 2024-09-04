package pt.fct.nova.id.srv.application.ontologies;

import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.iterator.ExtendedIterator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.apache.jena.ontology.OntModelSpec.OWL_MEM;
import static org.apache.jena.ontology.OntModelSpec.OWL_MEM_TRANS_INF;

public class DefaultOntology implements Ontology {
    private final Map<Node, Set<OntClass>> subClasses;
    private final Map<Node, Set<OntClass>> equivalentClasses;
    private final Map<Node, Restriction> classRestrictions;
    private final Map<Node, Set<? extends OntClass>> intersectionClasses;
    private final Map<Node, Set<OntClass>> intersectionsWhereClassIsOperand;
    private final Map<Node, Set<? extends OntProperty>> subProperties;
    private final Map<Node, Set<? extends OntProperty>> equivalentProperties;
    private final Map<Node, Set<? extends OntProperty>> inverseProperties;

    Set<Node> symmetricProperties = new HashSet<>();
    Set<Node> transitiveProperties = new HashSet<>();
    OntModel ontology;
    OntModelSpec spec;
    String triplestoreID;
    private final int transitivityDepth;
    private final int expansionDepth;


    public DefaultOntology(String triplestoreID, int t, int e) {
        this.subClasses = new HashMap<>();
        this.equivalentClasses = new HashMap<>();
        this.intersectionClasses = new HashMap<>();
        this.intersectionsWhereClassIsOperand = new HashMap<>();
        this.classRestrictions = new HashMap<>();
        this.subProperties = new HashMap<>();
        this.equivalentProperties = new HashMap<>();
        this.inverseProperties = new HashMap<>();
        this.transitivityDepth = t;
        this.expansionDepth = e;
        this.spec = OWL_MEM_TRANS_INF;
        this.triplestoreID = triplestoreID;
        this.ontology = ModelFactory.createOntologyModel(OWL_MEM);

    }

    public DefaultOntology(String triplestoreID) {
        this.subClasses = new HashMap<>();
        this.equivalentClasses = new HashMap<>();
        this.intersectionClasses = new HashMap<>();
        this.intersectionsWhereClassIsOperand = new HashMap<>();
        this.classRestrictions = new HashMap<>();
        this.subProperties = new HashMap<>();
        this.equivalentProperties = new HashMap<>();
        this.inverseProperties = new HashMap<>();
        this.transitivityDepth = 0;
        this.expansionDepth = 0;
        this.spec = OWL_MEM_TRANS_INF;
        this.triplestoreID = triplestoreID;
        this.ontology = ModelFactory.createOntologyModel(OWL_MEM);
    }

    @Override
    public void execInference(Set<Triple> schema, boolean inference) {
        OntModel tbox = ModelFactory.createOntologyModel(OWL_MEM);
        GraphUtil.add(tbox.getGraph(), schema.iterator());
        if (inference) {
            this.ontology = ModelFactory.createOntologyModel(spec, tbox);
            execClassInference();
            execPropertyInference();
        } else{
            this.ontology = tbox;
        }
    }

    private void execClassInference() {
        Set<OntClass> s;
        OntClass c;

        for (ExtendedIterator<OntClass> it = ontology.listClasses(); it.hasNext(); ) {
            c = it.next();
            s = c.listSubClasses().toSet();
            s.remove(c);
            subClasses.put(c.asNode(), s);
            s = c.listEquivalentClasses().toSet();
            s.remove(c);
            equivalentClasses.put(c.asNode(), s);

        }
        Restriction currentRestriction;
        for (ExtendedIterator<Restriction> it = ontology.listRestrictions(); it.hasNext(); ) {
            currentRestriction = it.next();
            if (currentRestriction.isSomeValuesFromRestriction())
                classRestrictions.put(currentRestriction.asNode(), currentRestriction);
            else if (currentRestriction.isHasValueRestriction())
                classRestrictions.put(currentRestriction.asNode(), currentRestriction);
        }
        IntersectionClass intersection;
        for (ExtendedIterator<IntersectionClass> it = ontology.listIntersectionClasses(); it.hasNext(); ) {
            intersection = it.next();
            Set<? extends OntClass> operands = intersection.listOperands().toSet();
            Set<OntClass> intersectionsDirectSuperclasses;
            for (OntClass operand : operands) {
                intersectionsDirectSuperclasses = intersectionsWhereClassIsOperand.get(operand.asNode());
                if (intersectionsDirectSuperclasses == null)
                    intersectionsDirectSuperclasses = new HashSet<>();
                intersectionsDirectSuperclasses.add(intersection);
                intersectionsWhereClassIsOperand.put(operand.asNode(), intersectionsDirectSuperclasses);
            }
            intersectionClasses.put(intersection.asNode(), operands);
        }
    }

    private void execPropertyInference() {
        Set<? extends OntProperty> s;
        for (OntProperty p : ontology.listAllOntProperties().toSet()) {
            extractPropertyInfo(p);
            s = p.listSubProperties().toSet();
            s.remove(p);
            subProperties.put(p.asNode(), s);
            s = p.listEquivalentProperties().toSet();
            s.remove(p);
            equivalentProperties.put(p.asNode(), s);
            s = p.listInverseOf().toSet();
            s.remove(p);
            inverseProperties.put(p.asNode(), s);
        }
    }

    private void extractPropertyInfo(OntProperty p) {
        if (p.isSymmetricProperty())
            symmetricProperties.add(p.asNode());
        else if (p.isTransitiveProperty())
            transitiveProperties.add(p.asNode());
    }

    @Override
    public Set<OntClass> getEquivalentClasses(Node node) {
        Set<OntClass> classes = equivalentClasses.get(node);
        return classes == null ? new HashSet<>() : classes;
    }

    @Override
    public Set<OntClass> getSubClasses(Node node) {
        Set<OntClass> classes = subClasses.get(node);
        return classes == null ? new HashSet<>() : classes;
    }

    @Override
    public Restriction getRestriction(Node node) {
        return classRestrictions.get(node);
    }

    @Override
    public Set<? extends OntClass> getIntersection(Node node) {
        Set<? extends OntClass> classes = intersectionClasses.get(node);
        return classes == null ? new HashSet<>() : classes;
    }

    @Override
    public Set<OntClass> getIntersectionWhereClassIsOperand(Node node) {
        Set<OntClass> classes = intersectionsWhereClassIsOperand.get(node);
        return classes == null ? new HashSet<>() : classes;
    }

    @Override
    public Set<? extends OntProperty> getSubProperties(Node node) {
        Set<? extends OntProperty> properties = subProperties.get(node);
        return properties == null ? new HashSet<>() : properties;
    }

    @Override
    public Set<? extends OntProperty> getEquivalentProperties(Node node) {
        Set<? extends OntProperty> properties = equivalentProperties.get(node);
        return properties == null ? new HashSet<>() : properties;
    }

    @Override
    public Set<? extends OntProperty> getInverseOf(Node node) {
        Set<? extends OntProperty> properties = inverseProperties.get(node);
        return properties == null ? new HashSet<>() : properties;
    }


    @Override
    public boolean isSymmetric(Node node) {
        return symmetricProperties.contains(node);
    }

    @Override
    public boolean isTransitive(Node node) {
        return transitiveProperties.contains(node);
    }

    @Override
    public Model getModel() {
        return ontology.difference(ModelFactory.createOntologyModel(OWL_MEM));
    }

    @Override
    public int getTransitivityDepth() {
        return transitivityDepth;
    }

    @Override
    public int getMaximumExpansionDepth() {
        return expansionDepth;
    }

}
