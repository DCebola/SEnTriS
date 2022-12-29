package pt.fct.nova.id.srv.presentation.controllers;

import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import java.util.List;

public class ParsingUtils {
    private static final Gson gson = new Gson();

    public static HttpEntity listToHttpEntity(List<String> searchResults) {
        return new StringEntity(gson.toJson(searchResults, List.class), ContentType.APPLICATION_JSON);
    }
}
