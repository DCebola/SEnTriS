package pt.fct.nova.id.srv.presentation.dtos;

import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

public class UpdateForm {
    @FormParam("uploads")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    private final byte[] uploads;
    @FormParam("deletions")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    private final byte[] deletions;

    public UpdateForm(byte[] uploads, byte[] deletions) {
        this.uploads = uploads;
        this.deletions = deletions;
    }

    public UpdateForm() {
        this.uploads = null;
        this.deletions = null;
    }

    public byte[] getUploads() {
        return uploads;
    }

    public byte[] getDeletions() {
        return deletions;
    }
}
