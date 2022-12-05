package pt.fct.nova.id.srv.presentation.api.dtos;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;
import pt.fct.nova.id.srv.application.protocols.ProtocolVersion;

import java.io.InputStream;

public class EncryptedCreateForm extends UploadForm {

    @FormParam("version")
    @DefaultValue("V1")
    @PartType(MediaType.TEXT_PLAIN)
    private final ProtocolVersion protocolVersion;

    public EncryptedCreateForm(String issuer, String storeID, String protocolVersion, String syntax, InputStream contents) {
        super(issuer, storeID, syntax, contents);
        this.protocolVersion = ProtocolVersion.fromString(protocolVersion);
    }

    public EncryptedCreateForm() {
        super();
        this.protocolVersion = null;

    }

    public ProtocolVersion getProtocolVersion() {
        return protocolVersion;
    }

}
