package pt.fct.nova.id.srv.presentation.api.dtos;

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


    public TriplestoreForm(String issuer, String triplestoreID) {
        this.issuer = issuer;
        this.triplestoreID = triplestoreID;

    }

    public TriplestoreForm() {
        this.issuer = null;
        this.triplestoreID = null;
    }

    public String getTriplestoreID() {
        return triplestoreID;
    }

    public String getIssuer() {
        return issuer;
    }

}
