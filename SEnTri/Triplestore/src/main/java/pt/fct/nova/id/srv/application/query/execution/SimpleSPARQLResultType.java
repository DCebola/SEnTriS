package pt.fct.nova.id.srv.application.query.execution;

import org.apache.jena.query.Query;
import org.apache.jena.query.SortCondition;

import java.util.List;

public class SimpleSPARQLResultType implements SPARQLResultType {

    private List<SortCondition> sortConditions;
    private boolean isDistinct;
    private boolean isOrdered;
    private boolean isSliced;
    private Long offset;
    private Long length;

    public SimpleSPARQLResultType(List<SortCondition> sortConditions, boolean isDistinct, boolean isOrdered, boolean isSliced, Long offset, Long length) {
        this.sortConditions = sortConditions;
        this.isDistinct = isDistinct;
        this.isOrdered = isOrdered;
        this.isSliced = isSliced;
        this.offset = offset;
        this.length = length;
    }

    public SimpleSPARQLResultType() {
        this.isDistinct = false;
        this.isOrdered = false;
        this.isSliced = false;
        this.offset = Query.NOLIMIT;
        this.length = Query.NOLIMIT;
    }

    public void setSortConditions(List<SortCondition> conditions) {
        if (sortConditions != null)
            sortConditions.addAll(conditions);
        else
            sortConditions = conditions;
    }

    public void setDistinct(boolean distinct) {
        isDistinct = distinct;
    }

    public void setOrdered(boolean ordered) {
        isOrdered = ordered;
    }

    public void setSliced(boolean sliced) {
        isSliced = sliced;
    }

    public void setOffset(Long offset) {
        this.offset = offset;
    }

    public void setLength(Long length) {
        this.length = length;
    }

    public List<SortCondition> getSortConditions() {
        return sortConditions;
    }

    public boolean isDistinct() {
        return isDistinct;
    }

    public boolean isOrdered() {
        return isOrdered;
    }

    public boolean isSliced() {
        return isSliced;
    }

    public Long getOffset() {
        return offset;
    }

    public Long getLength() {
        return length;
    }
}
