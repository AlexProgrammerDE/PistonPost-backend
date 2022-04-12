package net.pistonmaster.pistonpost.resources;

import com.codahale.metrics.annotation.Timed;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.dropwizard.auth.Auth;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.pistonpost.PistonPostApplication;
import net.pistonmaster.pistonpost.User;
import net.pistonmaster.pistonpost.api.SettingsResponse;
import net.pistonmaster.pistonpost.storage.UserDataStorage;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.glassfish.jersey.media.multipart.FormDataParam;

import static com.mongodb.client.model.Filters.eq;

@RequiredArgsConstructor
@Path("/settings")
public class SettingsResource {
    private final PistonPostApplication application;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public SettingsResponse getSettings(@Auth User user) {
        try (MongoClient mongoClient = application.createClient()) {
            MongoDatabase database = mongoClient.getDatabase("pistonpost");
            MongoCollection<UserDataStorage> collection = database.getCollection("users", UserDataStorage.class);

            Bson query = eq("_id", new ObjectId(user.getId()));
            UserDataStorage userData = collection.find(query).first();

            return new SettingsResponse(userData);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public void setSettings() {

    }

    @DELETE
    public void deleteAccount(@Auth User user) {
        try (MongoClient mongoClient = application.createClient()) {
            MongoDatabase database = mongoClient.getDatabase("pistonpost");
            MongoCollection<UserDataStorage> collection = database.getCollection("users", UserDataStorage.class);

            Bson query = eq("_id", new ObjectId(user.getId()));
            collection.deleteOne(query);
        }
    }
}
