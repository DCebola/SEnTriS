package pt.fct.nova.id.srv.application.query.execution;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import pt.fct.nova.id.srv.application.query.jobs.*;
import pt.fct.nova.id.srv.application.query.jobs.jobN.BGPJob;
import pt.fct.nova.id.srv.application.query.jobs.jobN.JobN;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.*;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.*;
import pt.fct.nova.id.srv.application.storage.StorageEngine;

import java.util.*;

import static pt.fct.nova.id.srv.application.query.jobs.VariablesPattern.*;


public class SimpleSPARQLWorker implements SPARQLWorker {

    private final StorageEngine storageEngine;
    private final String storeID;

    public SimpleSPARQLWorker(String storeID, StorageEngine storageEngine) {
        this.storeID = storeID;
        this.storageEngine = storageEngine;
    }

    @Override
    public Map<Var, Set<String>> exec(Job job) {
        if (job instanceof GetJob)
            return execGet((GetJob) job);
        else if (job instanceof ValuesJob)
            return execValues((ValuesJob) job);
        else if (job instanceof SliceJob)
            return execSlice((SliceJob) job);
        else
            return null;
    }

    private Map<Var, Set<String>> execGet(GetJob job) {
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
            case SPO -> storageEngine.findAll(storeID, Var.alloc(s), Var.alloc(p), Var.alloc(o));

        };
    }

    private Map<Var, Set<String>> fetchGetBindings(VariablesPattern varPattern, Var var, Node node1, Node node2) {
        Map<Var, Set<String>> bindings = new HashMap<>();
        if (varPattern == VariablesPattern.S)
            bindings.put(var, storageEngine.findSubjects(storeID, node1, node2));
        else if (varPattern == VariablesPattern.P)
            bindings.put(var, storageEngine.findPredicates(storeID, node1, node2));
        else if (varPattern == VariablesPattern.O)
            bindings.put(var, storageEngine.findObjects(storeID, node1, node2));
        return bindings;
    }

    private Map<Var, Set<String>> fetchGetBindings(VariablesPattern varPattern, Var var1, Var var2, Node node) {
        if (varPattern == VariablesPattern.SP) {
            return storageEngine.findSP(storeID, node, var1, var2);
        } else if (varPattern == VariablesPattern.SO) {
            return storageEngine.findSO(storeID, node, var1, var2);
        } else if (varPattern == VariablesPattern.PO) {
            return storageEngine.findPO(storeID, node, var1, var2);
        } else
            return new HashMap<>();
    }

    private Map<Var, Set<String>> execValues(ValuesJob job) {
        //TODO Execute ValuesJob
        return null;
    }

    private Map<Var, Set<String>> execSlice(SliceJob job) {
        //TODO Execute SliceJob
        return null;
    }


    @Override
    public Map<Var, Set<String>> exec(Job1 job, Map<Var, Set<String>> prevJobBindings) {
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

    private Map<Var, Set<String>> execProject(ProjectJob job, Map<Var, Set<String>> prevJobBindings) {
        prevJobBindings.keySet().retainAll(job.getVariables());
        return prevJobBindings;
    }

    private Map<Var, Set<String>> execBind(BindJob job, Map<Var, Set<String>> prevJobBindings) {
        //TODO Execute BindJob
        return null;
    }

    private Map<Var, Set<String>> execFilter(FilterJob job, Map<Var, Set<String>> prevJobBindings) {
        //TODO Execute FilterJob
        return null;
    }

    private Map<Var, Set<String>> execOrderBy(OrderByJob job, Map<Var, Set<String>> prevJobBindings) {
        //TODO Execute OrderByJob
        return null;
    }

    private Map<Var, Set<String>> execGroup(GroupJob job, Map<Var, Set<String>> prevJobBindings) {
        //TODO Execute GroupJob
        return null;
    }

    private Map<Var, Set<String>> execDistinct(DistinctJob job, Map<Var, Set<String>> prevJobBindings) {
        //TODO Execute DistinctJob
        return null;
    }

    @Override
    public Map<Var, Set<String>> exec(Job2 job, Map<Var, Set<String>> left, Map<Var, Set<String>> right) {
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
    public Map<Var, Set<String>> exec(JobN job, List<Map<Var, Set<String>>> prevJobsBindings) {
        if (job instanceof BGPJob)
            return execBGP((BGPJob) job, prevJobsBindings);
        else
            return null;
    }

    private Map<Var, Set<String>> execBGP(BGPJob job, List<Map<Var, Set<String>>> prevJobsBindings) {
        //TODO: Execute BGP
        return null;
    }

    private Map<Var, Set<String>> execJoin(JoinJob job, Map<Var, Set<String>> left, Map<Var, Set<String>> right) {
        Map<Var, Set<String>> join = new HashMap<>(left);
        right.forEach((k, v) -> join.merge(k, v, (l, r) -> {
            if (l.size() > r.size()) {
                l.retainAll(r);
                return l;
            } else {
                r.retainAll(l);
                return r;
            }
        }));
        return join;
    }

    private Map<Var, Set<String>> execUnion(UnionJob job, Map<Var, Set<String>> left, Map<Var, Set<String>> right) {

        //TODO Execute UnionJob
        return null;
    }

    private Map<Var, Set<String>> execOptional(OptionalJob job, Map<Var, Set<String>> left, Map<Var, Set<String>> right) {
        //TODO Execute OptionalJob
        return null;
    }

    private Map<Var, Set<String>> execMinus(MinusJob job, Map<Var, Set<String>> left, Map<Var, Set<String>> right) {
        //TODO Execute MinusJob
        return null;
    }

    @Override
    public List<Binding> generateBindings(Map<Var, Set<String>> jobBindings) {
        return storageEngine.getNodesAsBindings(storeID, jobBindings);
    }
}
