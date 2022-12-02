package pt.fct.nova.id.srv.presentation.api.dtos;

import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

import java.io.InputStream;

public class UploadForm extends TriplestoreForm {
    @FormParam("syntax")
    @PartType(MediaType.TEXT_PLAIN)
    private final String syntax;
    @FormParam("contents")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    private final InputStream contents;

    public UploadForm(String issuer, String storeID, String syntax, InputStream contents) {
        super(issuer, storeID);
        this.syntax = syntax;
        this.contents = contents;
    }
    public UploadForm() {
        super();
        this.syntax = null;
        this.contents = null;
    }

    public String getSyntax() {
        return syntax;
    }


    public InputStream getContents() {
        return contents;
    }
}
