package pt.fct.nova.id.srv.application.query.execution;

import org.apache.jena.query.Query;
import org.apache.jena.query.SortCondition;
import org.apache.jena.sparql.engine.binding.Binding;

import java.io.Serial;
import java.util.Collection;
import java.util.List;

public class DefaultSPARQLResult implements SPARQLResult {
    @Serial
    private static final long serialVersionUID = 5345654479992467695L;
    private List<SortCondition> sortConditions;
    private boolean isDistinct;
    private boolean isOrdered;
    private boolean isSliced;
    private Long offset;
    private Long length;

    private Collection<Binding> bindings;

    public DefaultSPARQLResult(List<SortCondition> sortConditions, boolean isDistinct, boolean isOrdered, boolean isSliced, Long offset, Long length) {
        this.sortConditions = sortConditions;
        this.isDistinct = isDistinct;
        this.isOrdered = isOrdered;
        this.isSliced = isSliced;
        this.offset = offset;
        this.length = length;
        this.bindings = null;
    }

    public DefaultSPARQLResult() {
        this.isDistinct = false;
        this.isOrdered = false;
        this.isSliced = false;
        this.offset = Query.NOLIMIT;
        this.length = Query.NOLIMIT;
        this.bindings = null;
    }

    @Override
    public void setBindings(Collection<Binding> bindings) {
        this.bindings = bindings;
    }

    public void setSortConditions(List<SortCondition> conditions) {
        if (sortConditions != null)
            sortConditions.addAll(conditions);
        else
            sortConditions = conditions;
    }
    @Override
    public void setDistinct(boolean distinct) {
        isDistinct = distinct;
    }
    @Override
    public void setOrdered(boolean ordered) {
        isOrdered = ordered;
    }
    @Override
    public void setSliced(boolean sliced) {
        isSliced = sliced;
    }
    @Override
    public void setOffset(Long offset) {
        this.offset = offset;
    }
    @Override
    public void setLength(Long length) {
        this.length = length;
    }
    @Override
    public List<SortCondition> getSortConditions() {
        return sortConditions;
    }
    @Override
    public boolean isDistinct() {
        return isDistinct;
    }
    @Override
    public boolean isOrdered() {
        return isOrdered;
    }
    @Override
    public boolean isSliced() {
        return isSliced;
    }
    @Override
    public Long getOffset() {
        return offset;
    }
    @Override
    public Long getLength() {
        return length;
    }

    @Override
    public Collection<Binding> getBindings() {
        return bindings;
    }
}
