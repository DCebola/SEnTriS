package pt.fct.nova.id.srv.application.query.execution;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import pt.fct.nova.id.srv.application.query.jobs.*;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.*;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.*;
import pt.fct.nova.id.srv.application.storage.StorageEngine;
import pt.fct.nova.id.srv.application.storage.dao.TypedNode;

import java.util.LinkedList;
import java.util.List;

import static pt.fct.nova.id.srv.application.query.jobs.VariablesPattern.*;


public class SimpleSPARQLWorker implements SPARQLWorker {

    private final StorageEngine storageEngine;
    private final String storeID;

    public SimpleSPARQLWorker(String storeID, StorageEngine storageEngine) {
        this.storeID = storeID;
        this.storageEngine = storageEngine;
    }

    @Override
    public List<Binding> exec(Job job) {
        if (job instanceof GetJob)
            return execGet((GetJob) job);
        else if (job instanceof ValuesJob)
            return execValues((ValuesJob) job);
        else if (job instanceof SliceJob)
            return execSlice((SliceJob) job);
        else
            return null;
    }

    private List<Binding> execGet(GetJob job) {
        Node s = job.getSubject();
        Node p = job.getPredicate();
        Node o = job.getObject();
        return switch (job.getVariablesPattern()) {
            case S -> fetchGetBindings(S, Var.alloc(s), p, o);
            case P -> fetchGetBindings(P, Var.alloc(p), s, o);
            case O -> fetchGetBindings(O, Var.alloc(o), s, p);
            case SP -> fetchGetBindings(SP, Var.alloc(s), Var.alloc(p), o);
            case SO -> fetchGetBindings(SO, Var.alloc(s), Var.alloc(o), p);
            case PO -> fetchGetBindings(PO, Var.alloc(p), Var.alloc(o), s);
            case SPO -> fetchGetBindings(storeID);
        };
    }

    private List<Binding> fetchGetBindings(String storeID) {
        List<Binding> res = new LinkedList<>();
        Var s = Var.alloc(S.name()), p = Var.alloc(P.name()), o = Var.alloc(O.name());
        storageEngine.getTriples(storeID).forEach(
                t -> {
                    res.add(BindingFactory.binding(s, t.getSubject()));
                    res.add(BindingFactory.binding(p, t.getPredicate()));
                    res.add(BindingFactory.binding(o, t.getObject()));
                }
        );
        return res;
    }

    private List<Binding> fetchGetBindings(VariablesPattern varPattern, Var var1, Var var2, Node node) {
        List<Binding> res = new LinkedList<>();
        Iterable<TypedNode> nodes = null;
        VariablesPattern t1 = null;

        if (varPattern == VariablesPattern.SP) {
            nodes = storageEngine.findSP(storeID, node);
            t1 = S;
        } else if (varPattern == VariablesPattern.SO) {
            nodes = storageEngine.findSO(storeID, node);
            t1 = S;
        } else if (varPattern == VariablesPattern.PO) {
            nodes = storageEngine.findPO(storeID, node);
            t1 = P;
        }
        if (nodes != null) {
            for (TypedNode n : nodes) {
                if (n.getType().equals(t1))
                    res.add(BindingFactory.binding(var1, n.getNode()));
                else
                    res.add(BindingFactory.binding(var2, n.getNode()));
            }
        }
        return res;
    }

    private List<Binding> fetchGetBindings(VariablesPattern varPattern, Var var, Node node1, Node node2) {
        List<Binding> res = new LinkedList<>();
        Iterable<Node> nodes = null;

        if (varPattern == VariablesPattern.S)
            nodes = storageEngine.findSubjects(storeID, node1, node2);
        else if (varPattern == VariablesPattern.P)
            nodes = storageEngine.findPredicates(storeID, node1, node2);
        else if (varPattern == VariablesPattern.O)
            nodes = storageEngine.findObjects(storeID, node1, node2);

        if (nodes != null)
            nodes.forEach(n -> res.add(BindingFactory.binding(var, n)));
        return res;
    }

    private List<Binding> execValues(ValuesJob job) {
        //TODO Execute ValuesJob
        return null;
    }

    private List<Binding> execSlice(SliceJob job) {
        //TODO Execute SliceJob
        return null;
    }


    @Override
    public List<Binding> exec(Job1 job, List<Binding> prevJobBindings) {
        if (job instanceof ProjectJob) {
            return execProject((ProjectJob) job, prevJobBindings);
        } else if (job instanceof BindJob) {
            return execBind((BindJob) job, prevJobBindings);
        } else if (job instanceof FilterJob) {
            return execFilter((FilterJob) job, prevJobBindings);
        } else if (job instanceof OrderByJob) {
            return execOrderBy((OrderByJob) job, prevJobBindings);
        } else if (job instanceof GroupJob) {
            return execGroup((GroupJob) job, prevJobBindings);
        } else if (job instanceof DistinctJob) {
            return execDistinct((DistinctJob) job, prevJobBindings);
        } else
            return null;
    }

    private List<Binding> execProject(ProjectJob job, List<Binding> prevJobBindings) {

        return null;
    }

    private List<Binding> execBind(BindJob job, List<Binding> prevJobBindings) {
        //TODO Execute BindJob
        return null;
    }

    private List<Binding> execFilter(FilterJob job, List<Binding> prevJobBindings) {
        //TODO Execute FilterJob
        return null;
    }

    private List<Binding> execOrderBy(OrderByJob job, List<Binding> prevJobBindings) {
        //TODO Execute OrderByJob
        return null;
    }

    private List<Binding> execGroup(GroupJob job, List<Binding> prevJobBindings) {
        //TODO Execute GroupJob
        return null;
    }

    private List<Binding> execDistinct(DistinctJob job, List<Binding> prevJobBindings) {
        //TODO Execute DistinctJob
        return null;
    }

    @Override
    public List<Binding> exec(Job2 job, List<Binding> left, List<Binding> right) {
        if (job instanceof JoinJob) {
            return execJoin((JoinJob) job, left, right);
        } else if (job instanceof UnionJob) {
            return execUnion((UnionJob) job, left, right);
        } else if (job instanceof OptionalJob) {
            return execOptional((OptionalJob) job, left, right);
        } else if (job instanceof MinusJob) {
            return execMinus((MinusJob) job, left, right);
        } else
            return null;
    }

    private List<Binding> execJoin(JoinJob job, List<Binding> left, List<Binding> right) {
        //TODO Execute JoinJob
        return null;
    }

    private List<Binding> execUnion(UnionJob job, List<Binding> left, List<Binding> right) {
        //TODO Execute UnionJob
        return null;
    }

    private List<Binding> execOptional(OptionalJob job, List<Binding> left, List<Binding> right) {
        //TODO Execute OptionalJob
        return null;
    }

    private List<Binding> execMinus(MinusJob job, List<Binding> left, List<Binding> right) {
        //TODO Execute MinusJob
        return null;
    }
}
