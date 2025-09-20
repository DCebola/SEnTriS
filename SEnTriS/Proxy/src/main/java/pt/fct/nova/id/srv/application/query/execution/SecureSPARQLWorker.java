package pt.fct.nova.id.srv.application.query.execution;

import pt.fct.nova.id.srv.application.query.execution.exceptions.JobInstanceException;
import pt.fct.nova.id.srv.application.query.execution.exceptions.SPARQLExecutionException;
import pt.fct.nova.id.srv.application.query.jobs.Job;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.*;
import pt.fct.nova.id.srv.application.query.jobs.jobs2.*;
import pt.fct.nova.id.srv.application.storage.tables.BindingsTable;

import java.util.*;

public abstract class SecureSPARQLWorker implements SPARQLWorker {

    private final SPARQLResult<byte[]> result;
    private final Set<String> allSearchIDs;

    public SecureSPARQLWorker() {
        result = new DefaultSPARQLResult<>();
        allSearchIDs = new HashSet<>();
    }

    public Set<String> getAllSearchIDs() {
        return allSearchIDs;
    }

    @Override
    public abstract BindingsTable exec(Job job) throws SPARQLExecutionException ;

    @Override
    public BindingsTable exec(Job1 job, BindingsTable prevJobResults) {
        if (job instanceof ProjectJob)
            return execProject((ProjectJob) job, prevJobResults);
        else if (job instanceof OrderByJob)
            return execOrderBy((OrderByJob) job, prevJobResults);
        else if (job instanceof DistinctJob)
            return execDistinct(prevJobResults);
        else if (job instanceof SliceJob)
            return execSlice((SliceJob) job, prevJobResults);
        throw new JobInstanceException(job.getClass().toString(), job.getID());

    }

    private BindingsTable execProject(ProjectJob job, BindingsTable prevJobResults) {
        System.out.println("PROJECT BEFORE: " + prevJobResults.getVars() + " | " + prevJobResults.getPatterns().size());
        prevJobResults.project(job.getVars());
        System.out.println("PROJECT AFTER: " + prevJobResults.getVars() + " | " + prevJobResults.getPatterns().size());
        return prevJobResults;
    }

    private BindingsTable execOrderBy(OrderByJob job, BindingsTable prevJobResults) {
        result.setOrdered(true);
        result.setSortConditions(job.getSortConditions());
        return prevJobResults;
    }

    private BindingsTable execSlice(SliceJob job, BindingsTable prevJobResults) {
        result.setSliced(true);
        result.setLength(job.getLength());
        result.setOffset(job.getOffset());
        return prevJobResults;
    }

    private BindingsTable execDistinct(BindingsTable prevJobResults) {
        result.setDistinct(true);
        return prevJobResults;
    }

    @Override
    public BindingsTable exec(Job2 job, BindingsTable left, BindingsTable right) throws SPARQLExecutionException {
        if (job instanceof JoinJob)
            return execJoin(left, right);
        else if (job instanceof UnionJob)
            return execUnion(left, right);
        else if (job instanceof OptionalJob)
            return execOptional(left, right);
        else if (job instanceof MinusJob)
            return execMinus(left, right);
        throw new JobInstanceException(job.getClass().toString(), job.getID());
    }

    private BindingsTable execJoin(BindingsTable left, BindingsTable right) {
        BindingsTable join = left.join(right);
        System.out.println("JOIN: " + join.getPatterns().size());
        System.out.println("[L] -" + Arrays.toString(left.getVars().toArray()));
        System.out.println("[R] -" + Arrays.toString(right.getVars().toArray()));
        return join;
    }

    private BindingsTable execUnion(BindingsTable left, BindingsTable right) {
        BindingsTable union = left.union(right);
        System.out.println("UNION: " + union.getPatterns().size());
        System.out.println("[L] -" + Arrays.toString(left.getVars().toArray()));
        System.out.println("[R] -" + Arrays.toString(right.getVars().toArray()));
        return union;
    }

    private BindingsTable execOptional(BindingsTable left, BindingsTable right) {
        BindingsTable optional = left.leftOuterJoin(right);
        System.out.println("OPTIONAL: " + optional.getPatterns().size());
        System.out.println("[L] -" + Arrays.toString(left.getVars().toArray()));
        System.out.println("[R] -" + Arrays.toString(right.getVars().toArray()));
        return optional;
    }

    private BindingsTable execMinus(BindingsTable left, BindingsTable right) {
        BindingsTable minus = left.minus(right);
        System.out.println("MINUS: " + minus.getPatterns().size());
        System.out.println("[L] -" + Arrays.toString(left.getVars().toArray()));
        System.out.println("[R] -" + Arrays.toString(right.getVars().toArray()));
        return minus;
    }

    @Override
    public abstract SPARQLResult<byte[]> generateResults(BindingsTable jobResults);

    public SPARQLResult<byte[]> getResult() {
        return result;
    }
}
