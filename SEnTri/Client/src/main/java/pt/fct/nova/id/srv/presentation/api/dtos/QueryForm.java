package pt.fct.nova.id.srv.presentation.api.dtos;

import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;
import pt.fct.nova.id.srv.presentation.api.RDFMediaType;

public class QueryForm extends TriplestoreForm {

    @FormParam("query")
    @PartType(RDFMediaType.SPARQL_QUERY)
    private final String query;

    @FormParam("syntax")
    @PartType(MediaType.TEXT_PLAIN)
    private final String syntax;

    public QueryForm(String issuer, String triplestoreID, String query, String syntax) {
        super(issuer, triplestoreID);
        this.query = query;
        this.syntax = syntax;
    }

    public QueryForm() {
        super();
        this.query = null;
        this.syntax = null;
    }

    public String getQuery() {
        return query;
    }

    public String getSyntax() {
        return syntax;
    }
}
