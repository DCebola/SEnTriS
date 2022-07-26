package pt.fct.nova.id.srv.application.query.execution;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingBuilder;
import pt.fct.nova.id.srv.application.query.jobs.*;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.*;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.*;
import pt.fct.nova.id.srv.application.storage.StorageEngine;
import pt.fct.nova.id.srv.application.storage.iri_tables.IRITable;
import pt.fct.nova.id.srv.application.storage.iri_tables.MemIRITable;

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
        if (job instanceof GetJob) return execGet((GetJob) job);
        else if (job instanceof EmptyResJob) return new MemIRITable(((EmptyResJob) job).getVars());
        else if (job instanceof ValuesJob) return execValues((ValuesJob) job);
        else if (job instanceof SliceJob) return execSlice((SliceJob) job);
        else return null;
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
        if (varPattern == VariablesPattern.S) return storageEngine.findSubjects(storeID, node1, node2, var);
        else if (varPattern == VariablesPattern.P) return storageEngine.findPredicates(storeID, node1, node2, var);
        else if (varPattern == VariablesPattern.O) return storageEngine.findObjects(storeID, node1, node2, var);
        return new MemIRITable();
    }

    private IRITable retrieveGetResults(VariablesPattern varPattern, Var var1, Var var2, Node node) {
        if (varPattern == VariablesPattern.SP) {
            return storageEngine.findSP(storeID, node, var1, var2);
        } else if (varPattern == VariablesPattern.SO) {
            return storageEngine.findSO(storeID, node, var1, var2);
        } else if (varPattern == VariablesPattern.PO) {
            return storageEngine.findPO(storeID, node, var1, var2);
        } else return new MemIRITable();
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
        } else return null;
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
        } else return null;
    }

    private IRITable execJoin(JoinJob job, IRITable left, IRITable right) {
        return left.join(right);
    }

    private IRITable execUnion(UnionJob job, IRITable left, IRITable right) {
        return left.union(right);
    }

    private IRITable execOptional(OptionalJob job, IRITable left, IRITable right) {
        return left.leftOuterJoin(right);
    }

    private IRITable execMinus(MinusJob job, IRITable left, IRITable right) {
        return left.minus(right);
    }

    @Override
    public List<Binding> generateBindings(IRITable jobResults) {
        List<Binding> res = new LinkedList<>();
        List<Var> vars = new ArrayList<>(jobResults.getVars());
        BindingBuilder builder = Binding.builder();
        int i;
        for (List<String> p_iris : jobResults.getPatterns()) {
            i = 0;
            for (String iri : p_iris) {
                if (iri != null)
                    builder.add(vars.get(i), storageEngine.generateNode(iri));
                i++;
            }
            res.add(builder.build());
            builder.reset();
        }

        return res;
    }
}
