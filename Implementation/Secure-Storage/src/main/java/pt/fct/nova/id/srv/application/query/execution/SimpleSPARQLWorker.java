package pt.fct.nova.id.srv.application.query.execution;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingBuilder;
import pt.fct.nova.id.srv.application.query.jobs.*;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.*;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.*;
import pt.fct.nova.id.srv.application.storage.StorageEngine;
import pt.fct.nova.id.srv.application.storage.dao.TypedNode;

import java.util.Iterator;
import java.util.Objects;

import static pt.fct.nova.id.srv.application.query.jobs.VariablesPattern.*;


public class SimpleSPARQLWorker implements SPARQLWorker {

    private final StorageEngine storageEngine;
    private final String storeID;

    public SimpleSPARQLWorker(String storeID, StorageEngine storageEngine) {
        this.storeID = storeID;
        this.storageEngine = storageEngine;
    }

    @Override
    public Binding exec(Job job) {
        if (job instanceof GetJob)
            return execGet((GetJob) job);
        else if (job instanceof ValuesJob)
            return execValues((ValuesJob) job);
        else if (job instanceof SliceJob)
            return execSlice((SliceJob) job);
        else
            return null;
    }

    private Binding execGet(GetJob job) {
        BindingBuilder builder = Binding.builder();
        switch (job.getVariablesPattern()) {
            case S -> updateGetBinding(builder, S, job.getPredicate(), job.getObject());
            case P -> updateGetBinding(builder, P, job.getSubject(), job.getObject());
            case O -> updateGetBinding(builder, O, job.getSubject(), job.getPredicate());
            case SP -> updateGetBinding(builder, SP, job.getObject());
            case SO -> updateGetBinding(builder, SO, job.getPredicate());
            case PO -> updateGetBinding(builder, PO, job.getSubject());
            case SPO -> storageEngine.getTriples(storeID).forEach(
                    t -> {
                        builder.add(Var.alloc(S.name()), t.getSubject());
                        builder.add(Var.alloc(P.name()), t.getPredicate());
                        builder.add(Var.alloc(O.name()), t.getObject());
                    }
            );
        }
        return builder.build();
    }

    private void updateGetBinding(BindingBuilder builder, VariablesPattern varPattern, Node node) {
        Iterable<TypedNode> nodes = null;

        if (varPattern == VariablesPattern.SP)
            nodes = storageEngine.findSP(node);
        else if (varPattern == VariablesPattern.SO)
            nodes = storageEngine.findSO(node);
        else if (varPattern == VariablesPattern.PO)
            nodes = storageEngine.findPO(node);

        if (nodes != null)
            nodes.forEach(
                    n -> builder.add(Var.alloc(n.getType().name()), n.getNode())
            );
    }

    private void updateGetBinding(BindingBuilder builder, VariablesPattern varPattern, Node node1, Node node2) {
        Iterable<Node> nodes = null;

        if (varPattern == VariablesPattern.S)
            nodes = storageEngine.findSubjects(node1, node2);
        else if (varPattern == VariablesPattern.P)
            nodes = storageEngine.findPredicates(node1, node2);
        else if (varPattern == VariablesPattern.O)
            nodes = storageEngine.findObjects(node1, node2);

        if (nodes != null)
            nodes.forEach(
                    n -> builder.add(Var.alloc(O.name()), n)
            );
    }

    private Binding execValues(ValuesJob job) {
        //TODO Execute ValuesJob
        return null;
    }

    private Binding execSlice(SliceJob job) {
        //TODO Execute SliceJob
        return null;
    }


    @Override
    public Binding exec(Job1 job, Binding prevJobBindings) {
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

    private Binding execProject(ProjectJob job, Binding prevJobBindings) {
        //TODO Execute GetJob
        return null;
    }

    private Binding execBind(BindJob job, Binding prevJobBindings) {
        //TODO Execute BindJob
        return null;
    }

    private Binding execFilter(FilterJob job, Binding prevJobBindings) {
        //TODO Execute FilterJob
        return null;
    }

    private Binding execOrderBy(OrderByJob job, Binding prevJobBindings) {
        //TODO Execute OrderByJob
        return null;
    }

    private Binding execGroup(GroupJob job, Binding prevJobBindings) {
        //TODO Execute GroupJob
        return null;
    }

    private Binding execDistinct(DistinctJob job, Binding prevJobBindings) {
        //TODO Execute DistinctJob
        return null;
    }

    @Override
    public Binding exec(Job2 job, Binding left, Binding right) {
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

    private Binding execJoin(JoinJob job, Binding left, Binding right) {
        //TODO Execute JoinJob
        return null;
    }

    private Binding execUnion(UnionJob job, Binding left, Binding right) {
        //TODO Execute UnionJob
        return null;
    }

    private Binding execOptional(OptionalJob job, Binding left, Binding right) {
        //TODO Execute OptionalJob
        return null;
    }

    private Binding execMinus(MinusJob job, Binding left, Binding right) {
        //TODO Execute MinusJob
        return null;
    }
}
