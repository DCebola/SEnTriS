package pt.fct.nova.id.srv.application.query.execution;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import pt.fct.nova.id.srv.application.query.jobs.*;
import pt.fct.nova.id.srv.application.query.jobs.jobN.BGPJob;
import pt.fct.nova.id.srv.application.query.jobs.jobN.JobN;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.*;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.*;
import pt.fct.nova.id.srv.application.storage.StorageEngine;
import pt.fct.nova.id.srv.application.storage.dao.TypedNode;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static pt.fct.nova.id.srv.application.query.jobs.VariablesPattern.*;


public class SimpleSPARQLWorker implements SPARQLWorker {

    private final StorageEngine storageEngine;
    private final String storeID;

    public SimpleSPARQLWorker(String storeID, StorageEngine storageEngine) {
        this.storeID = storeID;
        this.storageEngine = storageEngine;
    }

    @Override
    public Map<Var, List<Node>> exec(Job job) {
        if (job instanceof GetJob)
            return execGet((GetJob) job);
        else if (job instanceof ValuesJob)
            return execValues((ValuesJob) job);
        else if (job instanceof SliceJob)
            return execSlice((SliceJob) job);
        else
            return null;
    }

    private Map<Var, List<Node>> execGet(GetJob job) {
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
            case SPO -> storageEngine.findAll(storeID, Var.alloc(S.name()), Var.alloc(P.name()), Var.alloc(O.name()));

        };
    }

    private Map<Var, List<Node>> fetchGetBindings(VariablesPattern varPattern, Var var, Node node1, Node node2) {
        if (varPattern == VariablesPattern.S)
            return storageEngine.findSubjects(storeID, node1, node2, var);
        else if (varPattern == VariablesPattern.P)
            return storageEngine.findPredicates(storeID, node1, node2, var);
        else if (varPattern == VariablesPattern.O)
            return storageEngine.findObjects(storeID, node1, node2, var);
        else
            return null;
    }

    private Map<Var, List<Node>> fetchGetBindings(VariablesPattern varPattern, Var var1, Var var2, Node node) {
        if (varPattern == VariablesPattern.SP) {
            return storageEngine.findSP(storeID, node, var1, var2);
        } else if (varPattern == VariablesPattern.SO) {
            return storageEngine.findSO(storeID, node, var1, var2);
        } else if (varPattern == VariablesPattern.PO) {
            return storageEngine.findPO(storeID, node, var1, var2);
        } else
            return null;
    }

    private Map<Var, List<Node>> execValues(ValuesJob job) {
        //TODO Execute ValuesJob
        return null;
    }

    private Map<Var, List<Node>> execSlice(SliceJob job) {
        //TODO Execute SliceJob
        return null;
    }


    @Override
    public Map<Var, List<Node>> exec(Job1 job, Map<Var, List<Node>> prevJobBindings) {
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

    private Map<Var, List<Node>> execProject(ProjectJob job, Map<Var, List<Node>> prevJobBindings) {

        return null;
    }

    private Map<Var, List<Node>> execBind(BindJob job, Map<Var, List<Node>> prevJobBindings) {
        //TODO Execute BindJob
        return null;
    }

    private Map<Var, List<Node>> execFilter(FilterJob job, Map<Var, List<Node>> prevJobBindings) {
        //TODO Execute FilterJob
        return null;
    }

    private Map<Var, List<Node>> execOrderBy(OrderByJob job, Map<Var, List<Node>> prevJobBindings) {
        //TODO Execute OrderByJob
        return null;
    }

    private Map<Var, List<Node>> execGroup(GroupJob job, Map<Var, List<Node>> prevJobBindings) {
        //TODO Execute GroupJob
        return null;
    }

    private Map<Var, List<Node>> execDistinct(DistinctJob job, Map<Var, List<Node>> prevJobBindings) {
        //TODO Execute DistinctJob
        return null;
    }

    @Override
    public Map<Var, List<Node>> exec(Job2 job, Map<Var, List<Node>> left, Map<Var, List<Node>> right) {
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

    @Override
    public Map<Var, List<Node>> exec(JobN job, List<Map<Var, List<Node>>> prevJobsBindings) {
        if (job instanceof BGPJob)
            return execBGP((BGPJob) job, prevJobsBindings);
        else
            return null;
    }

    private Map<Var, List<Node>> execBGP(BGPJob job, List<Map<Var, List<Node>>> prevJobsBindings) {
        //TODO: Execute BGP
        return null;
    }

    private Map<Var, List<Node>> execJoin(JoinJob job, Map<Var, List<Node>> left, Map<Var, List<Node>> right) {
        //TODO Execute JoinJob
        return null;
    }

    private Map<Var, List<Node>> execUnion(UnionJob job, Map<Var, List<Node>> left, Map<Var, List<Node>> right) {
        //TODO Execute UnionJob
        return null;
    }

    private Map<Var, List<Node>> execOptional(OptionalJob job, Map<Var, List<Node>> left, Map<Var, List<Node>> right) {
        //TODO Execute OptionalJob
        return null;
    }

    private Map<Var, List<Node>> execMinus(MinusJob job, Map<Var, List<Node>> left, Map<Var, List<Node>> right) {
        //TODO Execute MinusJob
        return null;
    }
}
