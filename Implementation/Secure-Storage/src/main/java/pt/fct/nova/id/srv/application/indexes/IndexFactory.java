package pt.fct.nova.id.srv.application.indexes;

import jakarta.validation.constraints.NotNull;

public class IndexFactory {

    public static Index createIndex(IndexType type, long value) {
        return new Index(type, value, 0L);
    }

    public static Index createCompoundIndex(@NotNull Index idx1, @NotNull Index idx2) throws InvalidCompoundIndexException {
        if (idx1.isCompound() || idx2.isCompound())
            throw new InvalidCompoundIndexException();

        IndexType t, t1 = idx1.type(), t2 = idx2.type();

        if (t1 == IndexType.S)
            if (t2 == IndexType.P)
                t = IndexType.SP;
            else if (t2 == IndexType.O)
                t = IndexType.SO;
            else
                throw new InvalidCompoundIndexException();
        else if (t1 == IndexType.P)
            if (t2 == IndexType.O)
                t = IndexType.PO;
            else
                throw new InvalidCompoundIndexException();
        else
            throw new InvalidCompoundIndexException();
        return new Index(t, idx1.upper(), idx2.upper());
    }
}
