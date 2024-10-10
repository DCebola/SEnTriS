package pt.fct.nova.id.srv.presentation.api.dtos;

import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

public class PrepareSearchV2Form {
    @FormParam("mask")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    private final byte[] mask;

    @FormParam("n")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    private final byte[] n;
    @FormParam("trapdoors")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    private final byte[] trapdoors;

    public PrepareSearchV2Form(byte[] mask, byte[] trapdoors, byte[] n) {
        this.mask = mask;
        this.trapdoors = trapdoors;
        this.n = n;
    }

    public PrepareSearchV2Form() {
        this.mask = null;
        this.trapdoors = null;
        this.n = null;
    }

    public byte[] getMask() {
        return mask;
    }

    public byte[] getTrapdoors() {
        return trapdoors;
    }

    public byte[] getN() {
        return n;
    }

}
