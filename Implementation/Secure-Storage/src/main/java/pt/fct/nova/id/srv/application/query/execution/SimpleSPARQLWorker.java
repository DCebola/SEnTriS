package pt.fct.nova.id.srv.application.query.execution;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.graph.GraphFactory;
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
    public Map<Var, List<String>> exec(Job job) {
        if (job instanceof GetJob)
            return execGet((GetJob) job);
        else if (job instanceof ValuesJob)
            return execValues((ValuesJob) job);
        else if (job instanceof SliceJob)
            return execSlice((SliceJob) job);
        else
            return null;
    }

    private Map<Var, List<String>> execGet(GetJob job) {
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

    private Map<Var, List<String>> fetchGetBindings(VariablesPattern varPattern, Var var, Node node1, Node node2) {
        Map<Var, List<String>> bindings = new HashMap<>();
        if (varPattern == VariablesPattern.S)
            bindings.put(var, storageEngine.findSubjects(storeID, node1, node2));
        else if (varPattern == VariablesPattern.P)
            bindings.put(var, storageEngine.findPredicates(storeID, node1, node2));
        else if (varPattern == VariablesPattern.O)
            bindings.put(var, storageEngine.findObjects(storeID, node1, node2));
        return bindings;
    }

    private Map<Var, List<String>> fetchGetBindings(VariablesPattern varPattern, Var var1, Var var2, Node node) {
        if (varPattern == VariablesPattern.SP) {
            return storageEngine.findSP(storeID, node, var1, var2);
        } else if (varPattern == VariablesPattern.SO) {
            return storageEngine.findSO(storeID, node, var1, var2);
        } else if (varPattern == VariablesPattern.PO) {
            return storageEngine.findPO(storeID, node, var1, var2);
        } else
            return new HashMap<>();
    }

    private Map<Var, List<String>> execValues(ValuesJob job) {
        //TODO Execute ValuesJob
        return null;
    }

    private Map<Var, List<String>> execSlice(SliceJob job) {
        //TODO Execute SliceJob
        return null;
    }


    @Override
    public Map<Var, List<String>> exec(Job1 job, Map<Var, List<String>> prevJobBindings) {
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

    private Map<Var, List<String>> execProject(ProjectJob job, Map<Var, List<String>> prevJobBindings) {
        prevJobBindings.keySet().retainAll(job.getVariables());
        return prevJobBindings;
    }

    private Map<Var, List<String>> execBind(BindJob job, Map<Var, List<String>> prevJobBindings) {
        //TODO Execute BindJob
        return null;
    }

    private Map<Var, List<String>> execFilter(FilterJob job, Map<Var, List<String>> prevJobBindings) {
        //TODO Execute FilterJob
        return null;
    }

    private Map<Var, List<String>> execOrderBy(OrderByJob job, Map<Var, List<String>> prevJobBindings) {
        //TODO Execute OrderByJob
        return null;
    }

    private Map<Var, List<String>> execGroup(GroupJob job, Map<Var, List<String>> prevJobBindings) {
        //TODO Execute GroupJob
        return null;
    }

    private Map<Var, List<String>> execDistinct(DistinctJob job, Map<Var, List<String>> prevJobBindings) {
        //TODO Execute DistinctJob
        return null;
    }

    @Override
    public Map<Var, List<String>> exec(Job2 job, Map<Var, List<String>> left, Map<Var, List<String>> right) {
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
    public Map<Var, List<String>> exec(JobN job, List<Map<Var, List<String>>> prevJobsBindings) {
        if (job instanceof BGPJob)
            return execBGP((BGPJob) job, prevJobsBindings);
        else
            return null;
    }

    private Map<Var, List<String>> execBGP(BGPJob job, List<Map<Var, List<String>>> prevJobsBindings) {
        //TODO: Execute BGP
        return null;
    }

    private Map<Var, List<String>> execJoin(JoinJob job, Map<Var, List<String>> left, Map<Var, List<String>> right) {
        Var[] vars = filterNonMutualVars(left.keySet(), right.keySet());
        if (vars.length == 1)
            return join(vars[0], left, right);
        else if (vars.length == 2)
            return join(vars[0], vars[1], left, right);
        else if (vars.length == 3)
            return join(vars[0], vars[1], vars[0], left, right);
        else
            return new HashMap<>();
    }

    private Var[] filterNonMutualVars(Set<Var> left, Set<Var> right) {
        int l_size = left.size();
        int r_size = right.size();

        if (left.isEmpty() || right.isEmpty())
            return new Var[0];

        if (l_size >= r_size) {
            left.retainAll(right);
            l_size = r_size;
        } else
            right.retainAll(left);

        Var[] vars = new Var[l_size];
        int i = 0;
        for (Var v : left)
            vars[i++] = v;
        return vars;
    }

    private Map<Var, List<String>> join(Var var, Map<Var, List<String>> left, Map<Var, List<String>> right) {
        left.get(var).retainAll(right.get(var));
        return left;
    }

    private Map<Var, List<String>> join(Var var1, Var var2, Map<Var, List<String>> left, Map<Var, List<String>> right) {
        HashMap<Var, List<String>> res = new HashMap<>();





    }

    private Map<Var, List<String>> join(Var var1, Var var2, Var var3, Map<Var, List<String>> left, Map<Var, List<String>> right) {
    }


    private Map<Var, List<String>> execUnion(UnionJob job, Map<Var, List<String>> left, Map<Var, List<String>> right) {
        //TODO Execute UnionJob, need a set with O(1) for get at position i -> Use of an ArrayList or Array with the HashSet
        return null;
    }

    private Map<Var, List<String>> execOptional(OptionalJob job, Map<Var, List<String>> left, Map<Var, List<String>> right) {
        //TODO Execute OptionalJob
        return null;
    }

    private Map<Var, List<String>> execMinus(MinusJob job, Map<Var, List<String>> left, Map<Var, List<String>> right) {
        //TODO Execute MinusJob
        return null;
    }

    @Override
    public List<Binding> generateBindings(Map<Var, List<String>> jobBindings) {
        return storageEngine.getNodesAsBindings(storeID, jobBindings);
    }
}
