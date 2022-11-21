package pt.fct.nova.id.srv.presentation.api.dtos;

import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

import java.util.Map;

public class SecretsForm {

    @FormParam("issuer")
    @PartType(MediaType.TEXT_PLAIN)
    private final String issuer;

    @FormParam("storeID")
    @PartType(MediaType.TEXT_PLAIN)
    private final String storeID;

    @FormParam("secrets")
    @PartType(MediaType.APPLICATION_JSON)
    private final Map<String, String> secrets;

    public SecretsForm(String issuer, String storeID, Map<String, String> secrets) {
        this.issuer = issuer;
        this.storeID = storeID;
        this.secrets = secrets;
    }

    public SecretsForm() {
        this.issuer = null;
        this.storeID = null;
        this.secrets = null;
    }

    public String getStoreID() {
        return storeID;
    }

    public Map<String, String> getSecrets() {
        return secrets;
    }

    public String getIssuer() {
        return issuer;
    }
}
