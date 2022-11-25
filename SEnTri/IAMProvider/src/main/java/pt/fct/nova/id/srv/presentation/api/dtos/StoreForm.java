package pt.fct.nova.id.srv.presentation.api.dtos;

import org.jboss.resteasy.annotations.jaxrs.FormParam;

public class StoreForm {
    @FormParam("owner")
    private final String owner;

    @FormParam("storeID")
    private final String storeID;

    public StoreForm(String owner, String storeID) {
        this.owner = owner;
        this.storeID = storeID;
    }

    public StoreForm() {
        owner = null;
        storeID = null;
    }

    public String getOwner() {
        return owner;
    }

    public String getStoreID() {
        return storeID;
    }
}
