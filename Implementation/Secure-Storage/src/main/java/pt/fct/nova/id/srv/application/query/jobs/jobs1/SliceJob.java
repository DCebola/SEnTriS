package pt.fct.nova.id.srv.application.query.jobs.jobs1;

public class SliceJob extends BaseJob1 {

    private final long length;
    private final long offset;

    public SliceJob(String jobID, String prevJobID, long offset, long length) {
        super(jobID, prevJobID);
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
