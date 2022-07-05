package net.pistonmaster.pistonpost.resources;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.pistonpost.PistonPostApplication;
import net.pistonmaster.pistonpost.User;
import net.pistonmaster.pistonpost.api.PostResponse;
import net.pistonmaster.pistonpost.api.UserDataResponse;
import net.pistonmaster.pistonpost.api.UserPageResponse;
import net.pistonmaster.pistonpost.storage.PostStorage;
import net.pistonmaster.pistonpost.storage.SettingsStorage;
import net.pistonmaster.pistonpost.storage.UserDataStorage;
import org.bson.conversions.Bson;

import java.util.HashSet;
import java.util.Set;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Sorts.descending;

@RequiredArgsConstructor
@Path("/userdata")
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {
    private final PistonPostApplication application;

    @GET
    @Operation(
            summary = "Get public data of the current user",
            description = "Get public data of the current user. The frontend uses this to display the user's name and avatar.",
            tags = {"user"}
    )
    public UserDataResponse userData(@Parameter(hidden = true) @Auth User user) {
        return user.generateUserDataResponse();
    }

    @GET
    @Path("/{name}")
    @Operation(
            summary = "Get public data of a user",
            description = "Get public data of a user. If the user is not found, a 404 is returned.",
            tags = {"user"}
    )
    public UserPageResponse userData(@PathParam("name") String name) {
        MongoDatabase database = application.getDatabase("pistonpost");
        MongoCollection<UserDataStorage> collection = database.getCollection("users", UserDataStorage.class);
        MongoCollection<PostStorage> postCollection = database.getCollection("posts", PostStorage.class);

        Bson query = eq("name", name);

        UserDataStorage userData = collection.find(query).first();

        if (userData == null) {
            throw new WebApplicationException("User not found!", 404);
        }

        Set<PostResponse> storageResponse = new HashSet<>();
        for (PostStorage post : postCollection
                .find(eq("author", userData.getId()))
                .sort(descending("_id"))
                .limit(20)) {
            if (!post.isUnlisted()) {
                storageResponse.add(application.getPostFillerService().fillPostStorage(post, database));
            }
        }

        SettingsStorage settings = userData.getSettings();
        return new UserPageResponse(
                userData.getId().toHexString(),
                userData.getName(),
                new User(userData).getAvatar(),
                userData.getRoles(),
                settings != null ? settings.getBio() : null,
                settings != null ? settings.getWebsite() : null,
                settings != null ? settings.getLocation() : null,
                storageResponse
        );
    }
}
