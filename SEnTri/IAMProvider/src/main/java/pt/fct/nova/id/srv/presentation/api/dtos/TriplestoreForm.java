package pt.fct.nova.id.srv.presentation.api.dtos;

import org.jboss.resteasy.annotations.jaxrs.FormParam;

public class TriplestoreForm {
    @FormParam("issuer")
    private final String issuer;

    @FormParam("triplestoreID")
    private final String triplestoreID;

    public TriplestoreForm(String issuer, String triplestoreID) {
        this.issuer = issuer;
        this.triplestoreID = triplestoreID;
    }

    public TriplestoreForm() {
        issuer = null;
        triplestoreID = null;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getTriplestoreID() {
        return triplestoreID;
    }
}
