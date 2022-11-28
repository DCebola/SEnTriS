package pt.fct.nova.id.srv.presentation.api.dtos;

import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;
import pt.fct.nova.id.srv.presentation.api.RDFMediaType;

import java.util.Map;

public class SecureQueryForm extends StoreForm{
    @FormParam("secrets")
    @PartType(MediaType.APPLICATION_JSON)
    private final Map<String, String> secrets;

    @FormParam("query")
    @PartType(RDFMediaType.SPARQL_QUERY)
    private final String query;

    public SecureQueryForm(String issuer, String storeID, Map<String, String> secrets, String query) {
        super(issuer, storeID);
        this.query = query;
        this.secrets = secrets;

    }
    public SecureQueryForm() {
        super();
        this.secrets = null;
        this.query = null;
    }

    public Map<String, String> getSecrets() {
        return secrets;
    }

    public String getQuery() {
        return query;
    }

}
