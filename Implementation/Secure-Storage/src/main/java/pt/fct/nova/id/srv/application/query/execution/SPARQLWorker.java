package pt.fct.nova.id.srv.application.query.execution;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import pt.fct.nova.id.srv.application.query.jobs.Job;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.Job1;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.Job2;

import java.util.List;
import java.util.Map;

public interface SPARQLWorker {

    Map<Var, List<Node>> exec(Job job);

    Map<Var, List<Node>> exec(Job1 job, Map<Var, List<Node>> prevJobBindings);

    Map<Var, List<Node>> exec(Job2 job, Map<Var, List<Node>> left, Map<Var, List<Node>> right);

}
