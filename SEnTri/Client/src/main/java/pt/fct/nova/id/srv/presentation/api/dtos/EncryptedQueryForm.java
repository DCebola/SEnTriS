package pt.fct.nova.id.srv.presentation.api.dtos;

import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

import java.util.Map;

public class EncryptedQueryForm extends QueryForm{
    @FormParam("secrets")
    @PartType(MediaType.APPLICATION_JSON)
    private final Map<String, String> secrets;

    public EncryptedQueryForm(String issuer, String storeID, Map<String, String> secrets, String query) {
        super(issuer, storeID, query);
        this.secrets = secrets;

    }
    public EncryptedQueryForm() {
        super();
        this.secrets = null;
    }

    public Map<String, String> getSecrets() {
        return secrets;
    }

}
