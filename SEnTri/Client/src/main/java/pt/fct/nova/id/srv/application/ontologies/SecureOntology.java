package pt.fct.nova.id.srv.application.ontologies;

import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.iterator.ExtendedIterator;

import java.util.*;

import static org.apache.jena.ontology.OntModelSpec.OWL_MEM;
import static org.apache.jena.ontology.OntModelSpec.OWL_MEM_TRANS_INF;

public class SecureOntology implements Ontology {
    private final Map<Node, List<OntClass>> subClasses;
    private final Map<Node, List<OntClass>> equivalentClasses;
    private final Map<Node, Restriction> classRestrictions;
    private final Map<Node, List<? extends OntClass>> intersectionClasses;
    private final Map<Node, List<OntClass>> intersectionsWhereClassIsOperand;
    private final Map<Node, List<? extends OntProperty>> subProperties;
    private final Map<Node, List<? extends OntProperty>> equivalentProperties;
    private final Map<Node, List<? extends OntProperty>> inverseProperties;

    Set<Node> symmetricProperties = new HashSet<>();
    Set<Node> transitiveProperties = new HashSet<>();
    OntModel ontology;
    OntModelSpec spec;
    String triplestoreID;
    private final int transitivityDepth;
    private final int expansionDepth;


    public SecureOntology(String triplestoreID, int t, int e) {
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

    public SecureOntology(String triplestoreID) {
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
        } else {
            this.ontology = tbox;
        }

    }

    private void execClassInference() {
        List<OntClass> classes;
        OntClass c;
        for (ExtendedIterator<OntClass> it = ontology.listClasses(); it.hasNext(); ) {
            c = it.next();
            classes = c.listSubClasses().toList();
            classes.remove(c);
            Collections.shuffle(classes);
            subClasses.put(c.asNode(), classes);
            classes = c.listEquivalentClasses().toList();
            classes.remove(c);
            Collections.shuffle(classes);
            equivalentClasses.put(c.asNode(), classes);

        }
        Restriction currentRestriction;
        for (ExtendedIterator<Restriction> it = ontology.listRestrictions(); it.hasNext(); ) {
            currentRestriction = it.next();
            if (currentRestriction.isSomeValuesFromRestriction() || currentRestriction.isAllValuesFromRestriction() || currentRestriction.isSomeValuesFromRestriction())
                classRestrictions.put(currentRestriction.asNode(), currentRestriction);
        }
        IntersectionClass intersection;
        Map<Node, Set<OntClass>> aux = new HashMap<>();
        for (ExtendedIterator<IntersectionClass> it = ontology.listIntersectionClasses(); it.hasNext(); ) {
            intersection = it.next();
            List<? extends OntClass> operands = intersection.listOperands().toList();
            Collections.shuffle(operands);
            Set<OntClass> intersectionsDirectSuperclasses;
            for (OntClass operand : operands) {
                intersectionsDirectSuperclasses = aux.get(operand.asNode());
                if (intersectionsDirectSuperclasses == null)
                    intersectionsDirectSuperclasses = new HashSet<>();
                intersectionsDirectSuperclasses.add(intersection);
                aux.put(operand.asNode(), intersectionsDirectSuperclasses);
            }
            intersectionClasses.put(intersection.asNode(), operands);
        }
        aux.forEach((k, v) -> {
                    List<OntClass> l = new ArrayList<>(v);
                    Collections.shuffle(l);
                    intersectionsWhereClassIsOperand.put(k, l);
                }
        );
    }

    private void execPropertyInference() {
        List<? extends OntProperty> properties;
        for (OntProperty p : ontology.listAllOntProperties().toList()) {
            extractPropertyInfo(p);
            properties = new ArrayList<>(p.listSubProperties().toList());
            properties.remove(p);
            subProperties.put(p.asNode(), properties);
            properties = new ArrayList<>(p.listEquivalentProperties().toList());
            properties.remove(p);
            equivalentProperties.put(p.asNode(), properties);
            properties = new ArrayList<>(p.listInverseOf().toList());
            properties.remove(p);
            inverseProperties.put(p.asNode(), properties);
        }
    }

    private void extractPropertyInfo(OntProperty p) {
        if (p.isSymmetricProperty())
            symmetricProperties.add(p.asNode());
        else if (p.isTransitiveProperty())
            transitiveProperties.add(p.asNode());
    }

    @Override
    public List<OntClass> getEquivalentClasses(Node node) {
        List<OntClass> classes = equivalentClasses.get(node);
        return classes == null ? new ArrayList<>(0) : classes;
    }

    @Override
    public List<OntClass> getSubClasses(Node node) {
        List<OntClass> classes = subClasses.get(node);
        return classes == null ? new ArrayList<>(0) : classes;
    }

    @Override
    public Restriction getRestriction(Node node) {
        return classRestrictions.get(node);
    }

    @Override
    public List<? extends OntClass> getIntersection(Node node) {
        List<? extends OntClass> classes = intersectionClasses.get(node);
        return classes == null ? new ArrayList<>(0) : classes;
    }

    @Override
    public List<OntClass> getIntersectionWhereClassIsOperand(Node node) {
        List<OntClass> classes = intersectionsWhereClassIsOperand.get(node);
        return classes == null ? new ArrayList<>(0) : classes;
    }

    @Override
    public List<? extends OntProperty> getSubProperties(Node node) {
        List<? extends OntProperty> properties = subProperties.get(node);
        return properties == null ? new ArrayList<>(0) : properties;
    }

    @Override
    public List<? extends OntProperty> getEquivalentProperties(Node node) {
        List<? extends OntProperty> properties = equivalentProperties.get(node);
        return properties == null ? new ArrayList<>(0) : properties;
    }

    @Override
    public List<? extends OntProperty> getInverseOf(Node node) {
        List<? extends OntProperty> properties = inverseProperties.get(node);
        return properties == null ? new ArrayList<>(0) : properties;
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
