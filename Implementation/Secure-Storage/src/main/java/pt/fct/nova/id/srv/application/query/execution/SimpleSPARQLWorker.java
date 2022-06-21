package pt.fct.nova.id.srv.application.query.execution;

import org.apache.jena.sparql.engine.binding.Binding;
import pt.fct.nova.id.srv.application.query.jobs.GetJob;
import pt.fct.nova.id.srv.application.query.jobs.Job;
import pt.fct.nova.id.srv.application.query.jobs.SliceJob;
import pt.fct.nova.id.srv.application.query.jobs.ValuesJob;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.*;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.*;
import pt.fct.nova.id.srv.application.storage.StorageEngine;

public class SimpleSPARQLWorker implements SPARQLWorker {

    private final StorageEngine storageEngine;

    public SimpleSPARQLWorker(StorageEngine storageEngine) {
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
        //TODO Execute GetJob
        return null;
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
