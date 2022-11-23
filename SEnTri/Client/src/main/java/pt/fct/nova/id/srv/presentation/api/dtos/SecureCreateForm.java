package pt.fct.nova.id.srv.presentation.api.dtos;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;
import pt.fct.nova.id.srv.application.protocols.ProtocolVersion;

import java.io.InputStream;

public class SecureCreateForm {

    @FormParam("issuer")
    @PartType(MediaType.TEXT_PLAIN)
    private final String issuer;

    @FormParam("storeID")
    @PartType(MediaType.TEXT_PLAIN)
    private final String storeID;

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
        this.issuer = issuer;
        this.storeID = storeID;
        this.protocolVersion = ProtocolVersion.fromString(protocolVersion);
        this.syntax = syntax;
        this.contents = contents;
    }

    public SecureCreateForm() {
        this.issuer = null;
        this.storeID = null;
        this.protocolVersion = null;
        this.syntax = null;
        this.contents = null;
    }


    public String getSyntax() {
        return syntax;
    }


    public InputStream getContents() {
        return contents;
    }

    public ProtocolVersion getProtocolVersion() {
        return protocolVersion;
    }

    public String getStoreID() {
        return storeID;
    }

    public String getIssuer() {
        return issuer;
    }
}
