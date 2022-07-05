package net.pistonmaster.pistonpost.resources;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.pistonpost.PistonPostApplication;
import net.pistonmaster.pistonpost.api.PostResponse;
import net.pistonmaster.pistonpost.storage.PostStorage;
import net.pistonmaster.pistonpost.utils.MongoConstants;

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
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Get all posts that have a tag",
            description = "Get all posts that have a tag. Does not include private ones.",
            tags = {"tags"}
    )
    public List<PostResponse> getPost(@PathParam("tagName") String tagName) {
        MongoDatabase database = application.getDatabase("pistonpost");
        MongoCollection<PostStorage> collection = database.getCollection("posts", PostStorage.class);

        List<PostResponse> storageResponse = new ArrayList<>();
        for (PostStorage post : collection
                .find(in("tags", tagName))
                .collation(MongoConstants.CASE_INSENSITIVE)
                .sort(descending("_id"))
                .limit(40)) {
            if (!post.isUnlisted()) {
                storageResponse.add(application.getPostFillerService().fillPostStorage(post, database));
            }
        }

        return storageResponse;
    }
}
