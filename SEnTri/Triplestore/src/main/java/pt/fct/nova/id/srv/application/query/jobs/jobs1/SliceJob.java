package pt.fct.nova.id.srv.application.query.jobs.jobs1;

public class SliceJob extends BaseJob1 {

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
