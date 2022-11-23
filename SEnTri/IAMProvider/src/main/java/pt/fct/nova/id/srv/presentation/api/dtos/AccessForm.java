package pt.fct.nova.id.srv.presentation.api.dtos;

import jakarta.ws.rs.DefaultValue;
import org.jboss.resteasy.annotations.jaxrs.FormParam;

public class AccessForm extends StoreForm {
    @FormParam("write")
    @DefaultValue("false")
    private final boolean write;

    public AccessForm(String issuer, String storeID, boolean write) {
        super(issuer, storeID);
        this.write = write;
    }

    public AccessForm() {
        super();
        this.write = false;
    }

    public boolean getWrite() {
        return write;
    }
}
