package pt.fct.nova.id.srv.presentation.api.dtos;

import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

import java.io.InputStream;
import java.util.Map;

public class SecureUploadForm {
    @FormParam("password")
    @PartType(MediaType.TEXT_PLAIN)
    private final String password;
    @FormParam("syntax")
    @PartType(MediaType.TEXT_PLAIN)
    private final String syntax;

    @FormParam("contents")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    private final InputStream contents;

    public SecureUploadForm(String password, String syntax, InputStream contents) {
        this.password = password;
        this.syntax = syntax;
        this.contents = contents;
    }

    public SecureUploadForm() {
        this.password = null;
        this.syntax = null;
        this.contents = null;
    }

    public String getPassword() {
        return password;
    }

    public String getSyntax() {
        return syntax;
    }


    public InputStream getContents() {
        return contents;
    }

}
