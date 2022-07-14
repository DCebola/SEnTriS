package pt.fct.nova.id.srv.application.query.execution;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingBuilder;
import pt.fct.nova.id.srv.application.query.jobs.*;
import pt.fct.nova.id.srv.application.query.jobs.jobN.BGPJob;
import pt.fct.nova.id.srv.application.query.jobs.jobN.JobN;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.*;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.*;
import pt.fct.nova.id.srv.application.storage.StorageEngine;
import pt.fct.nova.id.srv.application.storage.iri_tables.IRITable;
import pt.fct.nova.id.srv.application.storage.iri_tables.MemIRITable;
import redis.clients.jedis.Response;

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
    public IRITable exec(Job job) {
        if (job instanceof GetJob)
            return execGet((GetJob) job);
        else if (job instanceof ValuesJob)
            return execValues((ValuesJob) job);
        else if (job instanceof SliceJob)
            return execSlice((SliceJob) job);
        else
            return null;
    }

    private IRITable execGet(GetJob job) {
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

    private IRITable retrieveGetResults(VariablesPattern varPattern, Var var, Node node1, Node node2) {
        if (varPattern == VariablesPattern.S)
            return storageEngine.findSubjects(storeID, node1, node2, var);
        else if (varPattern == VariablesPattern.P)
            return storageEngine.findPredicates(storeID, node1, node2, var);
        else if (varPattern == VariablesPattern.O)
            return storageEngine.findObjects(storeID, node1, node2, var);
        return new MemIRITable();
    }

    private IRITable retrieveGetResults(VariablesPattern varPattern, Var var1, Var var2, Node node) {
        if (varPattern == VariablesPattern.SP) {
            return storageEngine.findSP(storeID, node, var1, var2);
        } else if (varPattern == VariablesPattern.SO) {
            return storageEngine.findSO(storeID, node, var1, var2);
        } else if (varPattern == VariablesPattern.PO) {
            return storageEngine.findPO(storeID, node, var1, var2);
        } else
            return new MemIRITable();
    }

    private IRITable execValues(ValuesJob job) {
        //TODO Execute ValuesJob
        return null;
    }

    private IRITable execSlice(SliceJob job) {
        //TODO Execute SliceJob
        return null;
    }


    @Override
    public IRITable exec(Job1 job, IRITable prevJobResults) {
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

    private IRITable execProject(ProjectJob job, IRITable prevJobResults) {
        prevJobResults.project(job.getVariables());
        return prevJobResults;
    }

    private IRITable execBind(BindJob job, IRITable prevJobResults) {
        //TODO Execute BindJob
        return null;
    }

    private IRITable execFilter(FilterJob job, IRITable prevJobResults) {
        //TODO Execute FilterJob
        return null;
    }

    private IRITable execOrderBy(OrderByJob job, IRITable prevJobResults) {
        //TODO Execute OrderByJob
        return null;
    }

    private IRITable execGroup(GroupJob job, IRITable prevJobResults) {
        //TODO Execute GroupJob
        return null;
    }

    private IRITable execDistinct(DistinctJob job, IRITable prevJobResults) {
        //TODO Execute DistinctJob
        return null;
    }

    @Override
    public IRITable exec(Job2 job, IRITable left, IRITable right) {
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
    public IRITable exec(JobN job, List<IRITable> prevJobsBindings) {
        if (job instanceof BGPJob)
            return execBGP((BGPJob) job, prevJobsBindings);
        else
            return null;
    }

    private IRITable execBGP(BGPJob job, List<IRITable> prevJobsResults) {
        int num_jobs = prevJobsResults.size();
        Set<Var> all_vars = new HashSet<>();
        List<Set<Var>> result_vars = new ArrayList<>(num_jobs * 2);
        List<IRITable> joinResults = new ArrayList<>(num_jobs);
        Set<Integer> to_be_processed = new HashSet<>();

        Set<Var> vars;
        for (int i = 0; i < num_jobs; i++) {
            vars = prevJobsResults.get(i).getVars();
            all_vars.addAll(vars);
            result_vars.add(vars);
            to_be_processed.add(i);
        }

        int current, last = num_jobs, compatible = -1;
        boolean stop;
        IRITable t, t2, res = null;
        Set<Var> v2;
        while (!to_be_processed.isEmpty()) {
            stop = false;
            current = to_be_processed.iterator().next();
            for (Integer i : to_be_processed) {
                if (current != i) {
                    vars = result_vars.get(current);
                    v2 = result_vars.get(i);
                    for (Var v : vars) {
                        if (!v2.isEmpty() && v2.contains(v)) {
                            compatible = i;
                            if (current < num_jobs)
                                t = prevJobsResults.get(current);
                            else
                                t = joinResults.get(current - num_jobs);
                            if (i < num_jobs)
                                t2 = prevJobsResults.get(i);
                            else
                                t2 = joinResults.get(i - num_jobs);
                            res = t.join(t2);
                            joinResults.add(last - num_jobs, res);
                            result_vars.add(last, res.getVars());
                            v2.remove(v);
                            stop = true;
                            break;
                        }
                    }
                }
                if (stop) break;
            }
            if (res == null)
                return new MemIRITable(all_vars);
            to_be_processed.remove(current);
            to_be_processed.remove(compatible);
            if (!to_be_processed.isEmpty())
                to_be_processed.add(last);
            last++;
        }
        return res;
    }

    private IRITable execJoin(JoinJob job, IRITable left, IRITable right) {
        return left.join(right);
    }

    private IRITable execUnion(UnionJob job, IRITable left, IRITable right) {
        //TODO Execute UnionJob, need a set with O(1) for get at position i -> Use of an ArrayList or Array with the HashSet
        return left.union(right);
    }

    private IRITable execOptional(OptionalJob job, IRITable left, IRITable right) {
        //TODO Execute OptionalJob
        return left.leftOuterJoin(right);
    }

    private IRITable execMinus(MinusJob job, IRITable left, IRITable right) {
        //TODO Execute MinusJob
        return left.minus(right);
    }

    @Override
    public List<Binding> generateBindings(IRITable jobResults) {
        List<Binding> res = new LinkedList<>();
        Set<List<String>> patterns = jobResults.getPatterns();
        Set<Var> vars = jobResults.getVars();
        BindingBuilder builder = Binding.builder();
        for (List<String> p_iris : patterns) {
            for (String iri : p_iris)
                for (Var v : vars)
                    builder.add(v, storageEngine.generateNode(iri));
            res.add(builder.build());
            builder.reset();
        }
        return res;
    }
}
