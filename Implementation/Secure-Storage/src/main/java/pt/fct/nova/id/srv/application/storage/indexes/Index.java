package pt.fct.nova.id.srv.application.storage.indexes;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record Index(IndexType type, @Positive long upper, @PositiveOrZero long lower) {
    public boolean isCompound(){
        return lower != 0;
    }
}
