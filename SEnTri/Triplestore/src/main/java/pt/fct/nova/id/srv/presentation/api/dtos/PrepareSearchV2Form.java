package pt.fct.nova.id.srv.presentation.api.dtos;

import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

public class PrepareSearchV2Form {
    @FormParam("mask")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    private final byte[] mask;
    @FormParam("trapdoors")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    private final byte[] trapdoors;

    public PrepareSearchV2Form(byte[] mask, byte[] trapdoors) {
        this.mask = mask;
        this.trapdoors = trapdoors;
    }

    public PrepareSearchV2Form() {
        this.mask = null;
        this.trapdoors = null;
    }

    public byte[] getMask() {
        return mask;
    }


    public byte[] getTrapdoors() {
        return trapdoors;
    }
}
