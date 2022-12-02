package pt.fct.nova.id.srv.presentation.api.dtos;

import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;
import pt.fct.nova.id.srv.presentation.api.RDFMediaType;

public class QueryForm extends TriplestoreForm {

    @FormParam("query")
    @PartType(RDFMediaType.SPARQL_QUERY)
    private final String query;

    public QueryForm(String issuer, String storeID, String query) {
        super(issuer, storeID);
        this.query = query;
    }

    public QueryForm() {
        super();
        this.query = null;

    }

    public String getQuery() {
        return query;
    }
}
