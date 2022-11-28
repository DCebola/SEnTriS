package pt.fct.nova.id.srv.presentation.api.dtos;

import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

import java.io.InputStream;
import java.util.Map;

public class SecureUploadForm extends StoreForm{

    @FormParam("secrets")
    @PartType(MediaType.APPLICATION_JSON)
    private final Map<String, String> secrets;
    @FormParam("syntax")
    @PartType(MediaType.TEXT_PLAIN)
    private final String syntax;
    @FormParam("contents")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    private final InputStream contents;

    public SecureUploadForm(String issuer, String storeID, Map<String, String> secrets, String syntax, InputStream contents) {
        super(issuer, storeID);
        this.secrets = secrets;
        this.syntax = syntax;
        this.contents = contents;
    }

    public SecureUploadForm() {
        super();
        this.secrets = null;
        this.syntax = null;
        this.contents = null;
    }
    public Map<String, String> getSecrets() {
        return secrets;
    }

    public String getSyntax() {
        return syntax;
    }

    public InputStream getContents() {
        return contents;
    }
}
