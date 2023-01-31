package pt.fct.nova.id.srv.presentation.api.dtos;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;
import pt.fct.nova.id.srv.presentation.api.RDFMediaType;

public class QueryForm extends TriplestoreForm {
    @FormParam("query")
    @PartType(RDFMediaType.SPARQL_QUERY)
    private final String query;

    @FormParam("inference")
    @PartType(MediaType.TEXT_PLAIN)
    private final boolean inference;

    @FormParam("transitivityDepth")
    @PartType(MediaType.TEXT_PLAIN)
    private final int transitivityDepth;

    @FormParam("expansionDepth")
    @PartType(MediaType.TEXT_PLAIN)
    private final int expansionDepth;

    public QueryForm(String issuer, String triplestoreID, String query, String inference,
                     @DefaultValue("0") String transitivityDepth, @DefaultValue("0") String expansionDepth) {
        super(issuer, triplestoreID);
        this.query = query;
        this.inference = Boolean.parseBoolean(inference);
        this.transitivityDepth = Integer.parseInt(transitivityDepth);
        this.expansionDepth = Integer.parseInt(expansionDepth);
    }

    public QueryForm() {
        super();
        this.query = null;
        this.inference = false;
        this.transitivityDepth = 0;
        this.expansionDepth = 0;
    }

    public String getQuery() {
        return query;
    }

    public boolean getInference() {
        return inference;
    }

    public int getTransitivityDepth() {
        return transitivityDepth;
    }

    public int getExpansionDepth() {
        return expansionDepth;
    }
}
