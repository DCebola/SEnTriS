package pt.fct.nova.id.srv.presentation.dtos;

import org.apache.jena.riot.Lang;

import java.io.InputStream;
import java.util.Map;

public record UploadReqBody(Lang lang, Map<String, String> namespaces, InputStream contents) {
}
