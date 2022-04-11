package net.pistonmaster.pistonpost;

import com.codahale.metrics.health.HealthCheck;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import lombok.Getter;
import lombok.Setter;
import org.bson.BsonDocument;
import org.bson.BsonInt64;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;

import static com.mongodb.MongoClientSettings.getDefaultCodecRegistry;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class MongoManager extends HealthCheck {
    @Setter
    private String connectUri;
    private final CodecProvider pojoCodecProvider = PojoCodecProvider.builder().automatic(true).build();
    @Getter
    private final CodecRegistry pojoCodecRegistry = fromRegistries(getDefaultCodecRegistry(), fromProviders(pojoCodecProvider));

    public MongoClient createClient() {
        return MongoClients.create(
                MongoClientSettings.builder()
                        .applyConnectionString(new ConnectionString(connectUri))
                        .codecRegistry(pojoCodecRegistry)
                        .applicationName("PistonPost").build()
        );
    }

    @Override
    protected Result check() {
        try (MongoClient mongoClient = createClient()) {
            MongoDatabase database = mongoClient.getDatabase("admin");
            try {
                Bson command = new BsonDocument("ping", new BsonInt64(1));
                database.runCommand(command);
                return Result.healthy("Connected successfully to server.");
            } catch (MongoException me) {
                return Result.unhealthy(me);
            }
        }
    }
}
