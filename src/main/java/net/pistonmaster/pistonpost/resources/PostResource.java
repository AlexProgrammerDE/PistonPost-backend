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
import net.pistonmaster.pistonpost.api.PostCreateResponse;
import net.pistonmaster.pistonpost.api.PostResponse;
import net.pistonmaster.pistonpost.storage.PostStorage;
import net.pistonmaster.pistonpost.utils.IDGenerator;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.glassfish.jersey.media.multipart.FormDataParam;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

@RequiredArgsConstructor
@Path("/post")
public class PostResource {
    private final PistonPostApplication application;

    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public PostCreateResponse createPost(@Auth User user, @FormDataParam("title") String title, @FormDataParam("content") String content, @FormDataParam("tags") String tags, @FormDataParam("unlisted") String unlisted) {
        long timestamp = System.currentTimeMillis();

        if (title == null || content == null || tags == null || unlisted == null) {
            throw new WebApplicationException("Your request is missing data!", 400);
        }

        if (title.isBlank() || content.isBlank()) {
            throw new WebApplicationException("Your request is missing data!", 400);
        }

        if (title.length() > 100) {
            throw new WebApplicationException("Your title is too long!", 400);
        }

        if (content.length() > 1000) {
            throw new WebApplicationException("Your content is too long!", 400);
        }

        if (tags.isBlank()) {
            throw new WebApplicationException("You must have at least one tag!", 400);
        }

        title = title.trim();
        content = content.trim();
        tags = tags.trim();
        unlisted = unlisted.trim();

        String[] tagArray = tags.split(",");

        if (tagArray.length > 5) {
            throw new WebApplicationException("You can only have 5 tags!", 400);
        }

        if (tagArray.length == 0) {
            throw new WebApplicationException("You must have at least one tag!", 400);
        }

        List<String> tagList = new ArrayList<>();
        for (String tag : tagArray) {
            String trimmedTag = tag.trim();
            if (trimmedTag.length() > 20) {
                throw new WebApplicationException("Tags must be less than 20 characters!", 400);
            }
            tagList.add(trimmedTag.replace("  ", ""));
        }

        boolean unlistedBool = Boolean.parseBoolean(unlisted);

        String postId = IDGenerator.generateID();

        try (MongoClient mongoClient = application.createClient()) {
            MongoDatabase database = mongoClient.getDatabase("pistonpost");
            MongoCollection<PostStorage> collection = database.getCollection("posts", PostStorage.class);

            PostStorage post = new PostStorage(
                    new ObjectId(),
                    postId,
                    title,
                    content,
                    user.getId(),
                    tagList,
                    timestamp,
                    unlistedBool
            );

            collection.insertOne(post);

            return new PostCreateResponse(postId);
        }
    }

    @GET
    @Path("/{postId}")
    public PostResponse getPost(@PathParam("postId") String postId) {
        try (MongoClient mongoClient = application.createClient()) {
            MongoDatabase database = mongoClient.getDatabase("pistonpost");
            MongoCollection<PostStorage> collection = database.getCollection("posts", PostStorage.class);

            PostStorage post = collection.find(eq("postId", postId)).first();

            if (post == null) {
                throw new WebApplicationException("Post not found!", 404);
            }

            return application.getPostFillerService().fillPostStorage(post);
        }
    }

    @DELETE
    @Path("/{postId}")
    public void deletePost(@Auth User user, @PathParam("postId") String postId) {
        try (MongoClient mongoClient = application.createClient()) {
            MongoDatabase database = mongoClient.getDatabase("pistonpost");
            MongoCollection<PostStorage> collection = database.getCollection("posts", PostStorage.class);

            Bson query = eq("postId", postId);
            PostStorage post = collection.find(query).first();

            if (post == null) {
                throw new WebApplicationException("Post not found!", 404);
            }

            if (!post.getAuthor().toHexString().equals(user.getId().toHexString())) {
                throw new WebApplicationException("You can only delete your own posts!", 403);
            }

            collection.deleteOne(query);
        }
    }
}
