package pt.fct.nova.id.srv.application.querying.execution;

import pt.fct.nova.id.srv.application.querying.jobs.SerializableBinding;
import pt.fct.nova.id.srv.application.querying.jobs.SerializableSortCondition;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public interface SPARQLResult extends Serializable {

    void setBindings(Collection<SerializableBinding> bindings);

    void setSortConditions(List<SerializableSortCondition> sortConditions);

    void setDistinct(boolean distinct);

    void setOrdered(boolean ordered);

    void setSliced(boolean sliced);

    void setOffset(Long offset);

    void setLength(Long length);

    List<SerializableSortCondition> getSortConditions();

    boolean isDistinct();

    boolean isOrdered();

    boolean isSliced();

    Long getOffset();

    Long getLength();

    Collection<SerializableBinding> getBindings();
}
