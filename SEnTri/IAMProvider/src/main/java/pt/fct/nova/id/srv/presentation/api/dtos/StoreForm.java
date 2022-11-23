package pt.fct.nova.id.srv.presentation.api.dtos;

import org.jboss.resteasy.annotations.jaxrs.FormParam;

public class StoreForm {
    @FormParam("issuer")
    private final String issuer;

    @FormParam("store")
    private final String storeID;

    public StoreForm(String issuer, String storeID) {
        this.issuer = issuer;
        this.storeID = storeID;
    }

    public StoreForm() {
        this.issuer = null;
        this.storeID = null;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getStoreID() {
        return storeID;
    }
}
