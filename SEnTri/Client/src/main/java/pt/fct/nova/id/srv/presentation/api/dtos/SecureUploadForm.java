package pt.fct.nova.id.srv.presentation.api.dtos;

import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

import java.io.InputStream;
import java.util.Map;

public class SecureUploadForm {
    @FormParam("issuer")
    @PartType(MediaType.TEXT_PLAIN)
    private final String issuer;
    @FormParam("syntax")
    @PartType(MediaType.TEXT_PLAIN)
    private final String syntax;
    @FormParam("secrets")
    @PartType(MediaType.APPLICATION_JSON)
    private final Map<String, String> secrets;
    @FormParam("contents")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    private final InputStream contents;

    public SecureUploadForm(String issuer, String syntax, Map<String, String> secrets, InputStream contents) {
        this.syntax = syntax;
        this.issuer = issuer;
        this.secrets = secrets;
        this.contents = contents;
    }

    public SecureUploadForm() {
        this.issuer = null;
        this.secrets = null;
        this.syntax = null;
        this.contents = null;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getSyntax() {
        return syntax;
    }

    public InputStream getContents() {
        return contents;
    }

    public Map<String, String> getSecrets() {
        return secrets;
    }
}
