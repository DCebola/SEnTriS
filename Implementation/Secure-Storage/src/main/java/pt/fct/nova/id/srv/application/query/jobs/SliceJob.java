package pt.fct.nova.id.srv.application.query.jobs;

public class SliceJob extends BaseJob {

    private final long length;
    private final long offset;

    public SliceJob(String jobID, long offset, long length) {
        super(jobID);
        this.offset = offset;
        this.length = length;
    }

    public long getLength() {
        return length;
    }

    public long getOffset() {
        return offset;
    }
}
