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
import net.pistonmaster.pistonpost.storage.SettingsStorage;
import net.pistonmaster.pistonpost.storage.UserDataStorage;
import org.bson.conversions.Bson;
import org.glassfish.jersey.media.multipart.FormDataParam;

import static com.mongodb.client.model.Filters.eq;

@RequiredArgsConstructor
@Path("/settings")
public class SettingsResource {
    private final PistonPostApplication application;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public UserDataStorage getSettings(@Auth User user) {
        try (MongoClient mongoClient = application.createClient()) {
            MongoDatabase database = mongoClient.getDatabase("pistonpost");
            MongoCollection<UserDataStorage> collection = database.getCollection("users", UserDataStorage.class);

            Bson query = eq("_id", user.getId());

            return collection.find(query).first();
        }
    }

    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public void setSettings(@Auth User user, @FormDataParam("name") String name, @FormDataParam("bio") String bio, @FormDataParam("website") String website, @FormDataParam("location") String location, @FormDataParam("emailNotifications") String emailNotifications, @FormDataParam("theme") String theme) {
        try (MongoClient mongoClient = application.createClient()) {
            MongoDatabase database = mongoClient.getDatabase("pistonpost");
            MongoCollection<UserDataStorage> collection = database.getCollection("users", UserDataStorage.class);

            Bson query = eq("_id", user.getId());
            UserDataStorage userData = collection.find(query).first();

            if (userData != null) {
                SettingsStorage settings = userData.getSettings();

                if (settings == null) {
                    settings = new SettingsStorage();
                }

                if (!userData.getName().equals(name)) {
                    Bson newNameQuery = eq("name", name);
                    UserDataStorage newUserData = collection.find(newNameQuery).first();

                    if (newUserData != null) {
                        throw new WebApplicationException("Username already taken!", 409);
                    }

                    userData.setName(name);
                }

                validateBio(bio);
                validateWebsite(website);
                validateLocation(location);

                settings.setBio(bio);
                settings.setWebsite(website);
                settings.setLocation(location);
                settings.setEmailNotifications("true".equals(emailNotifications));
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

            Bson query = eq("_id", user.getId());
            collection.deleteOne(query);
        }
    }

    private void validateBio(String bio) {
        if (bio != null && bio.length() > 255) {
            throw new WebApplicationException("Your bio is too long!", 400);
        }
    }

    private void validateWebsite(String website) {
        if (website != null && website.length() > 80) {
            throw new WebApplicationException("Your website is too long!", 400);
        }

        if (website != null && !website.startsWith("https://")) {
            throw new WebApplicationException("Your website must start with \"https://\"!", 400);
        }
    }

    private void validateLocation(String location) {
        if (location != null && location.length() > 80) {
            throw new WebApplicationException("Your location is too long!", 400);
        }
    }
}
