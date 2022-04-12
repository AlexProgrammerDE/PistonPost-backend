package net.pistonmaster.pistonpost.resources;

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
import net.pistonmaster.pistonpost.storage.SettingsStorage;
import net.pistonmaster.pistonpost.storage.UserDataStorage;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

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
    public void setSettings(@Auth User user, @FormParam("username") String username, @FormParam("bio") String bio, @FormParam("emailNotifications") String emailNotifications, @FormParam("theme") String theme) {
        try (MongoClient mongoClient = application.createClient()) {
            MongoDatabase database = mongoClient.getDatabase("pistonpost");
            MongoCollection<UserDataStorage> collection = database.getCollection("users", UserDataStorage.class);

            Bson query = eq("_id", new ObjectId(user.getId()));
            UserDataStorage userData = collection.find(query).first();

            if (userData != null) {
                SettingsStorage settings = userData.getSettings();

                if (settings == null) {
                    settings = new SettingsStorage();
                }

                if (!userData.getName().equals(username)) {
                    Bson newNameQuery = eq("name", username);
                    UserDataStorage newUserData = collection.find(newNameQuery).first();

                    if (newUserData != null) {
                        throw new WebApplicationException("Username already taken", 409);
                    }

                    userData.setName(username);
                }

                settings.setBio(bio);
                settings.setEmailNotifications("on".equals(emailNotifications));
                settings.setTheme(theme);

                userData.setSettings(settings);

                collection.replaceOne(query, userData);
            }
        }
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
