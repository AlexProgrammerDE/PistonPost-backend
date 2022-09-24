package net.pistonmaster.pistonpost.resources;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotEmpty;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.pistonpost.PistonPostApplication;
import net.pistonmaster.pistonpost.User;
import net.pistonmaster.pistonpost.api.PostResponse;
import net.pistonmaster.pistonpost.storage.PostStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Sorts.descending;

@RequiredArgsConstructor
@Path("/home")
public class HomeResource {
    private static final int POSTS_PER_PAGE = 40;
    private final PistonPostApplication application;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Get posts for home page",
            description = "Get posts for home page. Can be accessed without bearer token.",
            tags = {"post"}
    )
    public List<PostResponse> getHomePosts(@Parameter(hidden = true) @Auth Optional<User> user, @QueryParam("page") @NotEmpty int page) {
        if (page < 0)
            page = 0;

        MongoDatabase database = application.getDatabase("pistonpost");
        MongoCollection<PostStorage> collection = database.getCollection("posts", PostStorage.class);

        List<PostResponse> storageResponse = new ArrayList<>();
        for (PostStorage post : collection.find().sort(descending("_id")).skip(page * POSTS_PER_PAGE).limit(POSTS_PER_PAGE)) {
            if (!post.isUnlisted()) {
                storageResponse.add(application.getPostFillerService().fillPostStorage(user.map(User::getId).orElse(null), post, database));
            }
        }

        return storageResponse;
    }
}
