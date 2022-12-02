package pt.fct.nova.id.srv.presentation.api.dtos;

import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

import java.util.Map;

public class SecretsForm {

    @FormParam("triplestoreID")
    @PartType(MediaType.TEXT_PLAIN)
    private final String triplestoreID;

    @FormParam("secrets")
    @PartType(MediaType.APPLICATION_JSON)
    private final Map<String, String> secrets;

    public SecretsForm(String triplestoreID, Map<String, String> secrets) {
        this.triplestoreID = triplestoreID;
        this.secrets = secrets;
    }

    public SecretsForm() {
        this.triplestoreID = null;
        this.secrets = null;
    }

    public String getTriplestoreID() {
        return triplestoreID;
    }

    public Map<String, String> getSecrets() {
        return secrets;
    }

}
