package pt.fct.nova.id.srv.presentation.controllers;


import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;

public class ParsingUtils {

    public static HttpEntity listToHttpEntity(List<byte[]> list) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(list);
            return new ByteArrayEntity(bos.toByteArray(), ContentType.APPLICATION_OCTET_STREAM);
        }
    }
}
