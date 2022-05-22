package net.pistonmaster.pistonpost.resources;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.pistonpost.PistonPostApplication;
import net.pistonmaster.pistonpost.User;
import net.pistonmaster.pistonpost.api.PostResponse;
import net.pistonmaster.pistonpost.storage.PostStorage;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Sorts.descending;

@RequiredArgsConstructor
@Path("/posts")
public class PostsResource {
    private final PistonPostApplication application;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Get all posts by the user",
            description = "Get all posts by the user",
            tags = {"post"}
    )
    public List<PostResponse> getAccountPosts(@Parameter(hidden = true) @Auth User user) {
        try (MongoClient mongoClient = application.createClient()) {
            MongoDatabase database = mongoClient.getDatabase("pistonpost");
            MongoCollection<PostStorage> collection = database.getCollection("posts", PostStorage.class);

            List<PostResponse> storageResponse = new ArrayList<>();
            for (PostStorage post : collection.find(eq("author", user.getId())).sort(descending("_id"))) {
                storageResponse.add(application.getPostFillerService().fillPostStorage(post));
            }

            return storageResponse;
        }
    }
}
