package pt.fct.nova.id.srv.presentation.api.dtos;

import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

import java.io.InputStream;
import java.util.Map;

public class SecureUploadForm extends UploadForm{

    @FormParam("secrets")
    @PartType(MediaType.APPLICATION_JSON)
    private final Map<String, String> secrets;

    public SecureUploadForm(String issuer, String storeID, Map<String, String> secrets, String syntax, InputStream contents) {
        super(issuer, storeID, syntax, contents);
        this.secrets = secrets;
    }

    public SecureUploadForm() {
        super();
        this.secrets = null;
    }
    public Map<String, String> getSecrets() {
        return secrets;
    }
}
