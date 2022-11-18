package pt.fct.nova.id.srv.presentation.api.dtos;

import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

import java.io.InputStream;

public class SecureUploadForm {

    @FormParam("syntax")
    @PartType(MediaType.TEXT_PLAIN)
    private final String syntax;

    @FormParam("batchSize")
    @PartType(MediaType.TEXT_PLAIN)
    private final Integer batchLength;

    @FormParam("contents")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    private final InputStream contents;

    public SecureUploadForm(String syntax, String batchLength, InputStream contents) {
        this.syntax = syntax;
        this.batchLength = Integer.parseInt(batchLength);
        this.contents = contents;
    }

    public SecureUploadForm() {
        this.batchLength = -1;
        this.syntax = null;
        this.contents = null;
    }

    public boolean isBatched() {
        return batchLength > 0;
    }

    public String getSyntax() {
        return syntax;
    }


    public InputStream getContents() {
        return contents;
    }

    public int getBatchLength() {
        return batchLength;
    }
}
