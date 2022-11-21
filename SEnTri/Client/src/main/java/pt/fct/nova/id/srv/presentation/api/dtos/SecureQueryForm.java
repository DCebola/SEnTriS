package pt.fct.nova.id.srv.presentation.api.dtos;

import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;
import pt.fct.nova.id.srv.presentation.api.RDFMediaType;

import java.util.Map;

public class SecureQueryForm {

    @FormParam("issuer")
    @PartType(MediaType.TEXT_PLAIN)
    private final String issuer;
    @FormParam("query")
    @PartType(RDFMediaType.SPARQL_QUERY)
    private final String query;

    @FormParam("secrets")
    @PartType(MediaType.APPLICATION_JSON)
    private final Map<String, String> secrets;

    public SecureQueryForm(String issuer, String query, Map<String, String> secrets) {
        this.issuer = issuer;
        this.query = query;
        this.secrets = secrets;

    }

    public SecureQueryForm() {
        this.issuer = null;
        this.secrets = null;
        this.query = null;
    }


    public String getQuery() {
        return query;
    }

    public Map<String, String> getSecrets() {
        return secrets;
    }

    public String getIssuer() {
        return issuer;
    }
}
