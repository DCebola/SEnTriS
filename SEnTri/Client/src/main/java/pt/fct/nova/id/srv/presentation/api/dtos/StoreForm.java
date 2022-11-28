package pt.fct.nova.id.srv.presentation.api.dtos;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

import java.io.InputStream;

public class StoreForm {
    @FormParam("issuer")
    @PartType(MediaType.TEXT_PLAIN)
    private final String issuer;

    @FormParam("storeID")
    @PartType(MediaType.TEXT_PLAIN)
    private final String storeID;

    public StoreForm(String issuer, String storeID) {
        this.issuer = issuer;
        this.storeID = storeID;
    }

    public StoreForm() {
        this.issuer = null;
        this.storeID = null;
    }

    public String getStoreID() {
        return storeID;
    }

    public String getIssuer() {
        return issuer;
    }
}
