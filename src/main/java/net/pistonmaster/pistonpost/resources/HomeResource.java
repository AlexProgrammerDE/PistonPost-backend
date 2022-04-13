package net.pistonmaster.pistonpost.resources;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.dropwizard.auth.Auth;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.pistonpost.PistonPostApplication;
import net.pistonmaster.pistonpost.User;
import net.pistonmaster.pistonpost.storage.PostStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Sorts.descending;
@RequiredArgsConstructor
@Path("/home")
public class HomeResource {
    private final PistonPostApplication application;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<PostStorage> getHomePosts(@Auth Optional<User> user) {
        try (MongoClient mongoClient = application.createClient()) {
            MongoDatabase database = mongoClient.getDatabase("pistonpost");
            MongoCollection<PostStorage> collection = database.getCollection("posts", PostStorage.class);

            List<PostStorage> storageResponse = new ArrayList<>();
            for (PostStorage post : collection.find().sort(descending("_id")).limit(40)) {
                if (!post.isUnlisted()) {
                    storageResponse.add(post);
                }
            }

            return storageResponse;
        }
    }
}
