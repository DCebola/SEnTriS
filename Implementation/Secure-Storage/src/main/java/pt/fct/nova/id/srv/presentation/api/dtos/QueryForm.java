package pt.fct.nova.id.srv.presentation.api.dtos;

import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

import java.io.InputStream;
import java.util.Map;

public class QueryForm extends UploadForm {
    @FormParam("protocol")
    @PartType(MediaType.TEXT_PLAIN)
    private final String protocol;

    public QueryForm(String protocol, String syntax, Map<String, String> namespaces, InputStream contents) {
        super(syntax, namespaces, contents);
        this.protocol = protocol;
    }

    public QueryForm() {
        super();
        this.protocol = null;
    }

    public String getProtocol() {
        return protocol;
    }
}
