package pt.fct.nova.id.srv.presentation.api.dtos;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;
import pt.fct.nova.id.srv.application.protocols.ProtocolVersion;

import java.io.InputStream;

public class SecureCreateForm extends StoreForm {

    @FormParam("version")
    @DefaultValue("V1")
    @PartType(MediaType.TEXT_PLAIN)
    private final ProtocolVersion protocolVersion;

    @FormParam("syntax")
    @PartType(MediaType.TEXT_PLAIN)
    private final String syntax;

    @FormParam("contents")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    private final InputStream contents;

    public SecureCreateForm(String issuer, String storeID, String protocolVersion, String syntax, InputStream contents) {
        super(issuer, storeID);
        this.syntax = syntax;
        this.contents = contents;
        this.protocolVersion = ProtocolVersion.fromString(protocolVersion);
    }

    public SecureCreateForm() {
        super();
        this.syntax = null;
        this.contents = null;
        this.protocolVersion = null;

    }

    public ProtocolVersion getProtocolVersion() {
        return protocolVersion;
    }

    public String getSyntax() {
        return syntax;
    }

    public InputStream getContents() {
        return contents;
    }

}
