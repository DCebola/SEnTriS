package pt.fct.nova.id.srv.presentation.controllers;

import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import java.util.List;

public class ParsingUtils {
    private static final Gson gson = new Gson();

    public static HttpEntity bindingsToHttpEntity(List<String> bindings) {
        return new StringEntity(gson.toJson(bindings, List.class), ContentType.APPLICATION_JSON);
    }
}
