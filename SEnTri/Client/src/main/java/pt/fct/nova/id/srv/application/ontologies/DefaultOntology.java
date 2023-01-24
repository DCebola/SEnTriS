package pt.fct.nova.id.srv.application.ontologies;

import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.PrintUtil;
import org.apache.jena.util.iterator.ExtendedIterator;
import pt.fct.nova.id.srv.application.query.jobs.SerializableBinding;

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
    private final Map<Node, Set<OntClass>> intersectionClasses;

    private Map<Node, Set<OntClass>> intersectionsWhereClassIsOperand;
    private final Map<Node, Set<? extends OntProperty>> subProperties;
    private final Map<Node, Set<? extends OntProperty>> equivalentProperties;
    private final Map<Node, Set<? extends OntProperty>> inverseProperties;
    private final HashMap<Node, OntClass> propertiesRange;

    Set<Node> symmetricProperties = new HashSet<>();
    Set<Node> transitiveProperties = new HashSet<>();
    OntModel ontology;
    OntModelSpec spec;
    String triplestoreID;
    private final int transitivityDepth = Integer.parseInt(System.getenv("TRANSITIVITY_DEPTH"));
    private final int expansionDepth = Integer.parseInt(System.getenv("EXPANSION_DEPTH"));


    public DefaultOntology(String triplestoreID, Set<Triple> schema, boolean inference) {
        this.subClasses = new HashMap<>();
        this.equivalentClasses = new HashMap<>();
        this.intersectionClasses = new HashMap<>();
        this.intersectionsWhereClassIsOperand = new HashMap<>();
        this.classRestrictions = new HashMap<>();
        this.subProperties = new HashMap<>();
        this.equivalentProperties = new HashMap<>();
        this.inverseProperties = new HashMap<>();
        this.propertiesRange = new HashMap<>();
        this.spec = OWL_MEM_TRANS_INF;
        this.triplestoreID = triplestoreID;
        OntModel tbox = ModelFactory.createOntologyModel(OWL_MEM);
        GraphUtil.add(tbox.getGraph(), schema.iterator());
        if (inference) {
            this.ontology = ModelFactory.createOntologyModel(spec, tbox);
            execClassInference();
            System.out.println("*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+");
            execPropertyInference();
            System.out.println("#################################+END OF ONTOLOGY+####################################");
        } else
            ontology = tbox;
    }

    private void execClassInference() {
        Set<OntClass> s;
        OntClass c;
        for (ExtendedIterator<OntClass> it = ontology.listClasses(); it.hasNext(); ) {
            c = it.next();
            System.out.println(PrintUtil.print(c));
            s = c.listSubClasses().toSet();
            s.remove(c);
            for (OntClass c2 : s)
                System.out.println(" s- " + PrintUtil.print(c2));
            subClasses.put(c.asNode(), s);
            s = c.listEquivalentClasses().toSet();
            s.remove(c);
            for (OntClass c2 : s)
                System.out.println(" eq- " + PrintUtil.print(c2));
            equivalentClasses.put(c.asNode(), s);

        }
        Restriction currentRestriction;
        for (ExtendedIterator<Restriction> it = ontology.listRestrictions(); it.hasNext(); ) {
            currentRestriction = it.next();
            if (currentRestriction.isSomeValuesFromRestriction()) {
                System.out.println(" r- " + PrintUtil.print(currentRestriction) + " | " + PrintUtil.print(currentRestriction.getOnProperty())
                        + " | " + PrintUtil.print(currentRestriction.asSomeValuesFromRestriction().getSomeValuesFrom()));
                classRestrictions.put(currentRestriction.asNode(), currentRestriction);
            } else if (currentRestriction.isHasValueRestriction()) {
                System.out.println(" r- " + PrintUtil.print(currentRestriction) + " | " + PrintUtil.print(currentRestriction.getOnProperty())
                        + " | " + PrintUtil.print(currentRestriction.asHasValueRestriction().getHasValue()));
                classRestrictions.put(currentRestriction.asNode(), currentRestriction);
            }
        }
        IntersectionClass intersection;
        for (ExtendedIterator<IntersectionClass> it = ontology.listIntersectionClasses(); it.hasNext(); ) {
            intersection = it.next();
            Set<OntClass> operands = new HashSet<>();
            System.out.println(" i- " + PrintUtil.print(intersection));
            for (OntClass ontClass : intersection.listOperands().toSet()) {
                System.out.println(" i-- " + PrintUtil.print(ontClass));
                operands.add(ontClass);
            }
            Set<OntClass> intersectionsDirectSuperclasses;
            for (OntClass operand: operands){
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
            System.out.println(PrintUtil.print(p));
            extractPropertyInfo(p);
            s = p.listSubProperties().toSet();
            s.remove(p);
            for (OntProperty p2 : s) {
                System.out.println(" su++ " + PrintUtil.print(p2));
            }
            subProperties.put(p.asNode(), s);
            s = p.listEquivalentProperties().toSet();
            s.remove(p);
            for (OntProperty p2 : s) {
                System.out.println(" eq++ " + PrintUtil.print(p2));
            }
            equivalentProperties.put(p.asNode(), s);
            s = p.listInverseOf().toSet();
            s.remove(p);
            for (OntProperty p2 : s) {
                System.out.println(" inv++ " + PrintUtil.print(p2));
            }
            inverseProperties.put(p.asNode(), s);
        }
    }

    private void extractPropertyInfo(OntProperty p) {
        OntResource range = p.getRange();
        if (range != null && range.isClass())
            propertiesRange.put(p.asNode(), range.asClass());
        if (p.isSymmetricProperty()) {
            System.out.println(" sy+ " + PrintUtil.print(p));
            symmetricProperties.add(p.asNode());
        } else if (p.isTransitiveProperty()) {
            System.out.println(" t+ " + PrintUtil.print(p));
            transitiveProperties.add(p.asNode());
        } else {
            System.out.println(" + " + PrintUtil.print(p));
        }
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
    public Set<OntClass> getIntersection(Node node) {
        Set<OntClass> classes = intersectionClasses.get(node);
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
    public Set<OntClass> getRange(Node node) {
        OntClass ontClass = propertiesRange.get(node);
        Set<OntClass> range = new HashSet<>();
        if (ontClass != null) {
            Set<OntClass> aux = getSubClasses(ontClass.asNode());
            if (subClasses != null)
                range.addAll(aux);
            aux = getEquivalentClasses(ontClass.asNode());
            if (aux != null)
                range.addAll(aux);
            range.add(ontClass);
        }
        return range;
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
