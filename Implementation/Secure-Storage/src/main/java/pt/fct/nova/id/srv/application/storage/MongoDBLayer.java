package pt.fct.nova.id.srv.application.storage;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.event.CommandListener;

import java.util.HashMap;
import java.util.Map;

public class MongoDBLayer {

    public static final String DB_NAME = System.getenv("DB_NAME");

    private static MongoDBLayer instance;
    private final Map<String, MongoClient> mongoClients;

    public static MongoDBLayer getInstance() {
        if (instance != null)
            return instance;

        instance = new MongoDBLayer();
        return instance;
    }

    public MongoDBLayer() {
        this.mongoClients = new HashMap<>();
    }

    public MongoClient getClient(String client, CommandListener commandListener) {
        MongoClient mongoClient = mongoClients.get(client);
        if (mongoClient == null) {
            MongoClientSettings settings =
                    MongoClientSettings.builder()
                            .applyConnectionString(new ConnectionString(System.getenv("DB_CONNECTION")))
                            .addCommandListener(commandListener)
                            .build();
            mongoClient = MongoClients.create(settings);
            mongoClients.put(client, mongoClient);
        }
        return mongoClient;
    }
}