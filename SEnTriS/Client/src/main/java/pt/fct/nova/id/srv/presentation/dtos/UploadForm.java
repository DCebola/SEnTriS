package pt.fct.nova.id.srv.presentation.dtos;

import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

import java.io.InputStream;

public class UploadForm extends SchemaForm {
    @FormParam("contents")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    private final InputStream content;

    public UploadForm(String issuer, String triplestoreID, String syntax, InputStream content) {
        super(issuer, triplestoreID, syntax);
        this.content = content;
    }
    public UploadForm() {
        super();
        this.content = null;
    }

    public InputStream getContent() {
        return content;
    }

}
