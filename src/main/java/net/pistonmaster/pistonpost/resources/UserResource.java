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
import net.pistonmaster.pistonpost.api.UserDataResponse;
import net.pistonmaster.pistonpost.storage.UserDataStorage;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import static com.mongodb.client.model.Filters.eq;

@RequiredArgsConstructor
@Path("/userdata")
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {
    private final PistonPostApplication application;
    @GET
    @Timed
    public UserDataResponse userData(@Auth User user) {
        return user.generateUserDataResponse();
    }

    @GET
    @Path("/{id}")
    @Timed
    public UserDataResponse userData(@PathParam("id") String id) {
        try (MongoClient mongoClient = application.createClient()) {
            MongoDatabase database = mongoClient.getDatabase("pistonpost");
            MongoCollection<UserDataStorage> collection = database.getCollection("users", UserDataStorage.class);

            Bson query = eq("_id", new ObjectId(id));

            UserDataStorage userData = collection.find(query).first();

            if (userData == null) {
                throw new WebApplicationException("User not found!", 404);
            }

            return new User(userData).generateUserDataResponse();
        }
    }
}
