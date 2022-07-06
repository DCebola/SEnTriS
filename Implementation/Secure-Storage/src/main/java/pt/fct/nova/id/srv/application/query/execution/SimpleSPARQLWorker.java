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
import pt.fct.nova.id.srv.application.storage.idx_data_structs.IdxTable;
import pt.fct.nova.id.srv.application.storage.idx_data_structs.MemIdxTable;

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
    public IdxTable exec(Job job) {
        if (job instanceof GetJob)
            return execGet((GetJob) job);
        else if (job instanceof ValuesJob)
            return execValues((ValuesJob) job);
        else if (job instanceof SliceJob)
            return execSlice((SliceJob) job);
        else
            return null;
    }

    private IdxTable execGet(GetJob job) {
        Node s = job.getSubject();
        Node p = job.getPredicate();
        Node o = job.getObject();
        return switch (job.getVariablesPattern()) {
            case S -> retrieveGetResults(S, Var.alloc(s), p, o);
            case P -> retrieveGetResults(P, Var.alloc(p), s, o);
            case O -> retrieveGetResults(O, Var.alloc(o), s, p);
            case SP -> retrieveGetResults(SP, Var.alloc(s), Var.alloc(p), o);
            case SO -> retrieveGetResults(SO, Var.alloc(s), Var.alloc(o), p);
            case PO -> retrieveGetResults(PO, Var.alloc(p), Var.alloc(o), s);
            case SPO -> storageEngine.findAll(storeID, Var.alloc(s), Var.alloc(p), Var.alloc(o));

        };
    }

    private IdxTable retrieveGetResults(VariablesPattern varPattern, Var var, Node node1, Node node2) {
        if (varPattern == VariablesPattern.S)
            return storageEngine.findSubjects(storeID, node1, node2, var);
        else if (varPattern == VariablesPattern.P)
            return storageEngine.findPredicates(storeID, node1, node2, var);
        else if (varPattern == VariablesPattern.O)
            return storageEngine.findObjects(storeID, node1, node2, var);
        return new MemIdxTable();
    }

    private IdxTable retrieveGetResults(VariablesPattern varPattern, Var var1, Var var2, Node node) {
        if (varPattern == VariablesPattern.SP) {
            return storageEngine.findSP(storeID, node, var1, var2);
        } else if (varPattern == VariablesPattern.SO) {
            return storageEngine.findSO(storeID, node, var1, var2);
        } else if (varPattern == VariablesPattern.PO) {
            return storageEngine.findPO(storeID, node, var1, var2);
        } else
            return new MemIdxTable();
    }

    private IdxTable execValues(ValuesJob job) {
        //TODO Execute ValuesJob
        return null;
    }

    private IdxTable execSlice(SliceJob job) {
        //TODO Execute SliceJob
        return null;
    }


    @Override
    public IdxTable exec(Job1 job, IdxTable prevJobResults) {
        if (job instanceof ProjectJob) {
            return execProject((ProjectJob) job, prevJobResults);
        } else if (job instanceof BindJob) {
            return execBind((BindJob) job, prevJobResults);
        } else if (job instanceof FilterJob) {
            return execFilter((FilterJob) job, prevJobResults);
        } else if (job instanceof OrderByJob) {
            return execOrderBy((OrderByJob) job, prevJobResults);
        } else if (job instanceof GroupJob) {
            return execGroup((GroupJob) job, prevJobResults);
        } else if (job instanceof DistinctJob) {
            return execDistinct((DistinctJob) job, prevJobResults);
        } else
            return null;
    }

    private IdxTable execProject(ProjectJob job, IdxTable prevJobResults) {
        prevJobResults.project(job.getVariables());
        return prevJobResults;
    }

    private IdxTable execBind(BindJob job, IdxTable prevJobResults) {
        //TODO Execute BindJob
        return null;
    }

    private IdxTable execFilter(FilterJob job, IdxTable prevJobResults) {
        //TODO Execute FilterJob
        return null;
    }

    private IdxTable execOrderBy(OrderByJob job, IdxTable prevJobResults) {
        //TODO Execute OrderByJob
        return null;
    }

    private IdxTable execGroup(GroupJob job, IdxTable prevJobResults) {
        //TODO Execute GroupJob
        return null;
    }

    private IdxTable execDistinct(DistinctJob job, IdxTable prevJobResults) {
        //TODO Execute DistinctJob
        return null;
    }

    @Override
    public IdxTable exec(Job2 job, IdxTable left, IdxTable right) {
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
    public IdxTable exec(JobN job, List<IdxTable> prevJobsBindings) {
        if (job instanceof BGPJob)
            return execBGP((BGPJob) job, prevJobsBindings);
        else
            return null;
    }

    private IdxTable execBGP(BGPJob job, List<IdxTable> prevJobsResults) {
        int num_jobs = prevJobsResults.size() - 2;
        IdxTable res = prevJobsResults.get(0);
        res = res.join(prevJobsResults.get(1));
        for (int i = 1; i < num_jobs; i++)
            res = res.join(prevJobsResults.get(i));
        return res;
    }

    private IdxTable execJoin(JoinJob job, IdxTable left, IdxTable right) {
        return left.join(right);
    }

    private IdxTable execUnion(UnionJob job, IdxTable left, IdxTable right) {
        //TODO Execute UnionJob, need a set with O(1) for get at position i -> Use of an ArrayList or Array with the HashSet
        return left.union(right);
    }

    private IdxTable execOptional(OptionalJob job, IdxTable left, IdxTable right) {
        //TODO Execute OptionalJob
        return left.leftOuterJoin(right);
    }

    private IdxTable execMinus(MinusJob job, IdxTable left, IdxTable right) {
        //TODO Execute MinusJob
        return left.minus(right);
    }

    @Override
    public List<Binding> generateBindings(IdxTable jobResults) {
        return storageEngine.fetchNodes(storeID, jobResults);
    }
}
