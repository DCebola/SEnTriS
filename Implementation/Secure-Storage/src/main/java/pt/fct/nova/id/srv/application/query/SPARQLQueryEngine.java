package pt.fct.nova.id.srv.application.query;

import org.apache.jena.query.*;
import org.apache.jena.sparql.algebra.*;
import pt.fct.nova.id.srv.application.query.execution.SPARQLExecution;
import pt.fct.nova.id.srv.application.query.execution.SimpleSPARQLExecution;
import pt.fct.nova.id.srv.application.query.plans.ExecutionPlan;
import pt.fct.nova.id.srv.application.query.plans.SPARQLPlanner;
import pt.fct.nova.id.srv.application.query.plans.SimpleSPARQLPlanner;

public class SPARQLQueryEngine implements QueryEngine {

    private final AlgebraGenerator algebraGenerator;
    private final SPARQLPlanner planner;

    public SPARQLQueryEngine() {
        this.algebraGenerator = new AlgebraGenerator(ARQ.getContext());
        this.planner = new SimpleSPARQLPlanner();
    }

    @Override
    public ResultSet execQuery(String queryString) {
        Query query = QueryFactory.create(queryString);
        ExecutionPlan plan = planner.generatePlan(algebraGenerator.compile(query));
        new SimpleSPARQLExecution(plan).exec();
        plan.getExecutionOrder().forEach(System.out::println);
        plan.getJobs().forEach((j, k) -> System.out.println("[" + j + " " + k + "]"));
        return null;
    }
}
