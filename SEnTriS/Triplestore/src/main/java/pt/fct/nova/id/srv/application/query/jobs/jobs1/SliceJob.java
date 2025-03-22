package pt.fct.nova.id.srv.application.query.jobs.jobs1;

import java.io.Serial;

public class SliceJob extends BaseJob1 {
    @Serial
    private static final long serialVersionUID = 5545662348392523294L;
    private final Long length;
    private final Long offset;

    public SliceJob(String jobID, String prevJobID, Long offset, Long length) {
        super(jobID, prevJobID);
        this.offset = offset;
        this.length = length;
    }

    public Long getLength() {
        return length;
    }

    public Long getOffset() {
        return offset;
    }
}
