package pt.fct.nova.id.srv.presentation.api.dtos;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;


public class TriplestoreForm {
    @FormParam("issuer")
    @PartType(MediaType.TEXT_PLAIN)
    private final String issuer;

    @FormParam("triplestoreID")
    @PartType(MediaType.TEXT_PLAIN)
    private final String triplestoreID;

    @FormParam("isSchema")
    @DefaultValue("false")
    @PartType(MediaType.TEXT_PLAIN)
    private final boolean isSchema;

    public TriplestoreForm(String issuer, String triplestoreID, String isSchema) {
        this.issuer = issuer;
        this.triplestoreID = triplestoreID;
        this.isSchema = Boolean.parseBoolean(isSchema);
    }

    public TriplestoreForm() {
        this.issuer = null;
        this.triplestoreID = null;
        isSchema = false;
    }

    public String getTriplestoreID() {
        return triplestoreID;
    }

    public String getIssuer() {
        return issuer;
    }

    public boolean isSchema() {
        return isSchema;
    }
}
