package pt.fct.nova.id.srv.application.querying.plans;

import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.AlgebraGenerator;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.update.Update;
import pt.fct.nova.id.srv.application.querying.QueryType;

import java.util.Collection;

import java.util.List;


public interface SPARQLPlanner {
    QueryExecutionPlan generatePlan(Op op) throws NotImplemented;

    QueryExecutionPlan generatePlan(Update op, AlgebraGenerator algebraGenerator) throws NotImplemented;
    void setQueryType(QueryType queryType);
    pt.fct.nova.id.srv.application.querying.QueryType getQueryType();

    void setConstructTemplate(List<Triple> constructTemplate);

    List<Triple> getConstructTemplate();
    Collection<Triple> getUploadTemplate();
    Collection<Triple> getDeleteTemplate();

}
