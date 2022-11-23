package pt.fct.nova.id.srv.presentation.api.dtos;

import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

import java.io.InputStream;
import java.util.Map;

public class UploadForm {
    @FormParam("syntax")
    @PartType(MediaType.TEXT_PLAIN)
    private final String syntax;
    @FormParam("namespaces")
    @PartType(MediaType.APPLICATION_JSON)
    private final Map<String, String> namespaces;
    @FormParam("contents")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    private final InputStream contents;

    public UploadForm(String syntax, Map<String, String> namespaces, InputStream contents) {
        this.syntax = syntax;
        this.namespaces = namespaces;
        this.contents = contents;
    }
    public UploadForm() {
        this.syntax = null;
        this.namespaces = null;
        this.contents = null;
    }

    public String getSyntax() {
        return syntax;
    }

    public Map<String, String> getNamespaces() {
        return namespaces;
    }

    public InputStream getContents() {
        return contents;
    }
}
