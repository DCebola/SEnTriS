package pt.fct.nova.id.srv.presentation.api.dtos;

import org.jboss.resteasy.annotations.jaxrs.FormParam;

public class TriplestoreForm {
    @FormParam("owner")
    private final String owner;

    @FormParam("triplestoreID")
    private final String triplestoreID;

    public TriplestoreForm(String owner, String triplestoreID) {
        this.owner = owner;
        this.triplestoreID = triplestoreID;
    }

    public TriplestoreForm() {
        owner = null;
        triplestoreID = null;
    }

    public String getOwner() {
        return owner;
    }

    public String getTriplestoreID() {
        return triplestoreID;
    }
}
