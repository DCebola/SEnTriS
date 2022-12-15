package pt.fct.nova.id.srv.application.query.execution;

import org.apache.jena.query.SortCondition;
import org.apache.jena.sparql.engine.binding.Binding;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public interface SPARQLResult extends Serializable {

    void setBindings(Collection<Binding> bindings);

    void setSortConditions(List<SortCondition> sortConditions);

    void setDistinct(boolean distinct);

    void setOrdered(boolean ordered);

    void setSliced(boolean sliced);

    void setOffset(Long offset);

    void setLength(Long length);

    List<SortCondition> getSortConditions();

    boolean isDistinct();

    boolean isOrdered();

    boolean isSliced();

    Long getOffset();

    Long getLength();

    Collection<Binding> getBindings();
}
