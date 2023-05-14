package pt.fct.nova.id.srv.presentation.api.dtos;

import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

public class SchemaForm extends TriplestoreForm{
    @FormParam("syntax")
    @PartType(MediaType.TEXT_PLAIN)
    private final String syntax;

    public SchemaForm(String issuer, String triplestoreID, String syntax) {
        super(issuer, triplestoreID);
        this.syntax = syntax;
    }

    public SchemaForm() {
        super();
        syntax = null;
    }

    public String getSyntax() {
        return syntax;
    }
}
