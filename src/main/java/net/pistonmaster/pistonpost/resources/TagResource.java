package net.pistonmaster.pistonpost.resources;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.pistonpost.PistonPostApplication;
import net.pistonmaster.pistonpost.storage.PostStorage;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Sorts.descending;

@RequiredArgsConstructor
@Path("/tag")
@Produces(MediaType.APPLICATION_JSON)
public class TagResource {
    private final PistonPostApplication application;

    @GET
    @Path("/{tagName}")
    public List<PostStorage> getPost(@PathParam("tagName") String tagName) {
        try (MongoClient mongoClient = application.createClient()) {
            MongoDatabase database = mongoClient.getDatabase("pistonpost");
            MongoCollection<PostStorage> collection = database.getCollection("posts", PostStorage.class);

            List<PostStorage> storageResponse = new ArrayList<>();
            for (PostStorage post : collection
                    .find(in("tags", tagName))
                    .sort(descending("_id"))
                    .limit(40)) {
                if (!post.isUnlisted()) {
                    storageResponse.add(post);
                }
            }

            return storageResponse;
        }
    }
}
