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
import net.pistonmaster.pistonpost.api.PostCreateResponse;
import net.pistonmaster.pistonpost.api.PostResponse;
import net.pistonmaster.pistonpost.manager.StaticFileManager;
import net.pistonmaster.pistonpost.storage.CommentStorage;
import net.pistonmaster.pistonpost.storage.PostStorage;
import net.pistonmaster.pistonpost.utils.IDGenerator;
import net.pistonmaster.pistonpost.utils.PostType;
import net.pistonmaster.pistonpost.utils.VoteType;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;

import java.util.*;

import static com.mongodb.client.model.Filters.eq;

@RequiredArgsConstructor
@Path("/post")
public class PostResource {
    private static final int MAX_IMAGES = 150;
    private final PistonPostApplication application;
    private final StaticFileManager staticFileManager;

    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Create a new post",
            description = "Create a new post.",
            tags = {"post"}
    )
    public PostCreateResponse createPost(@Parameter(hidden = true) @Auth User user, @FormDataParam("title") String title, @FormDataParam("type") PostType type, @FormDataParam("tags") String tags, @FormDataParam("unlisted") String unlisted, FormDataMultiPart multiPart) {
        long timestamp = System.currentTimeMillis();

        if (title == null || type == null || tags == null || unlisted == null) {
            throw new WebApplicationException("Your request is missing data!", 400);
        }

        validateTitle(title);
        validateTags(tags);

        title = title.trim();
        tags = tags.trim();
        unlisted = unlisted.trim();

        MongoDatabase database = application.getDatabase("pistonpost");

        String content = null;
        List<ObjectId> imageIds = new ArrayList<>();
        ObjectId videoId = null;

        switch (type) {
            case TEXT -> {
                content = multiPart.getField("content").getValue();
                validateContent(content);
                content = content.trim();
            }
            case IMAGES -> {
                List<FormDataBodyPart> imageParts = multiPart.getFields("image");
                if (imageParts.size() > MAX_IMAGES) {
                    throw new WebApplicationException("You can only upload a maximum of " + MAX_IMAGES + " images!", 400);
                }
                for (FormDataBodyPart body : imageParts) {
                    imageIds.add(staticFileManager.uploadImage(database, body.getValueAs(byte[].class), body.getContentDisposition()));
                }
                if (imageIds.isEmpty()) {
                    throw new WebApplicationException("Your request is missing data!", 400);
                }
            }
            case VIDEO -> {
                FormDataBodyPart body = multiPart.getField("video");
                if (body == null) {
                    throw new WebApplicationException("Your request is missing data!", 400);
                }
                videoId = staticFileManager.uploadVideo(database, body.getValueAs(byte[].class), body.getContentDisposition());
            }
        }

        List<String> tagList = parseTags(tags);

        boolean unlistedBool = Boolean.parseBoolean(unlisted);

        String postId = IDGenerator.generateID();

        MongoCollection<PostStorage> collection = database.getCollection("posts", PostStorage.class);

        PostStorage post = new PostStorage(
                new ObjectId(),
                postId,
                title,
                type,
                content,
                imageIds,
                videoId,
                user.getId(),
                tagList,
                List.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                timestamp,
                unlistedBool
        );

        collection.insertOne(post);

        return new PostCreateResponse(postId);
    }

    @GET
    @Path("/{postId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Get a post",
            description = "Get a post.",
            tags = {"post"}
    )
    public PostResponse getPost(@Parameter(hidden = true) @Auth Optional<User> user, @PathParam("postId") String postId) {
        MongoDatabase database = application.getDatabase("pistonpost");
        MongoCollection<PostStorage> collection = database.getCollection("posts", PostStorage.class);

        PostStorage post = collection.find(eq("postId", postId)).first();

        if (post == null) {
            throw new WebApplicationException("Post not found!", 404);
        }

        System.out.println(user);
        return application.getPostFillerService().fillPostStorage(user.map(User::getId).orElse(null), post, database);
    }

    @PUT
    @Path("/{postId}")
    @Operation(
            summary = "Edit a post",
            description = "Edit a post.",
            tags = {"post"}
    )
    public void editPost(@Parameter(hidden = true) @Auth User user, @PathParam("postId") String postId, @FormDataParam("title") String title, @FormDataParam("tags") String tags, @FormDataParam("unlisted") String unlisted, FormDataMultiPart multiPart) {
        if (title == null || tags == null || unlisted == null) {
            throw new WebApplicationException("Your request is missing data!", 400);
        }

        validateTitle(title);
        validateTags(tags);

        title = title.trim();
        tags = tags.trim();
        unlisted = unlisted.trim();

        boolean unlistedBool = Boolean.parseBoolean(unlisted);

        List<String> tagList = parseTags(tags);

        MongoDatabase database = application.getDatabase("pistonpost");
        MongoCollection<PostStorage> collection = database.getCollection("posts", PostStorage.class);

        Bson query = eq("postId", postId);
        PostStorage post = collection.find(query).first();

        if (post == null) {
            throw new WebApplicationException("Post not found!", 404);
        }

        if (!user.getRoles().contains("ADMIN")
                && !post.getAuthor().equals(user.getId())) {
            throw new WebApplicationException("You can only edit your own posts!", 403);
        }

        String content = null;
        switch (post.getType()) {
            case TEXT -> {
                content = multiPart.getField("content").getValue();
                validateContent(content);
                content = content.trim();
            }
            case IMAGES, VIDEO -> {
            }
        }

        post.setTitle(title);

        if (content != null) {
            post.setContent(content);
        }

        post.setUnlisted(unlistedBool);
        post.setTags(tagList);

        collection.replaceOne(query, post);
    }

    @DELETE
    @Path("/{postId}")
    @Operation(
            summary = "Delete a post",
            description = "Delete a post.",
            tags = {"post"}
    )
    public void deletePost(@Parameter(hidden = true) @Auth User user, @PathParam("postId") String postId) {
        MongoDatabase database = application.getDatabase("pistonpost");
        MongoCollection<PostStorage> collection = database.getCollection("posts", PostStorage.class);

        Bson query = eq("postId", postId);
        PostStorage post = collection.find(query).first();

        if (post == null) {
            throw new WebApplicationException("Post not found!", 404);
        }

        if (!user.getRoles().contains("ADMIN")
                && !post.getAuthor().equals(user.getId())) {
            throw new WebApplicationException("You can only delete your own posts!", 403);
        }

        collection.deleteOne(query);
    }

    private void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new WebApplicationException("Your request is missing data!", 400);
        }

        if (title.length() > 100) {
            throw new WebApplicationException("Your title is too long!", 400);
        }
    }

    private void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new WebApplicationException("Your request is missing data!", 400);
        }

        if (content.length() > 1000) {
            throw new WebApplicationException("Your content is too long!", 400);
        }
    }

    private void validateComment(String content) {
        if (content == null || content.isBlank()) {
            throw new WebApplicationException("Your request is missing data!", 400);
        }

        if (content.length() > 250) {
            throw new WebApplicationException("Your content is too long!", 400);
        }
    }

    private void validateTags(String tags) {
        if (tags == null || tags.isBlank()) {
            throw new WebApplicationException("You must have at least one tag!", 400);
        }
    }

    private List<String> parseTags(String tags) {
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

            if (trimmedTag.matches("^[a-zA-Z\\d-._~]+$")) {
                tagList.add(trimmedTag);
            } else {
                throw new WebApplicationException("Your tag contains invalid characters! (Allowed: a-z, A-Z, 0-9 and -._~)", 400);
            }
        }
        return tagList;
    }

    @PUT
    @Path("/{postId}/comment")
    @Operation(
            summary = "Add a comment to a post",
            description = "Add a comment to a post.",
            tags = {"post"}
    )
    public void commentCreate(@Parameter(hidden = true) @Auth User user, @PathParam("postId") String postId, @FormDataParam("content") String content) {
        if (content == null) {
            throw new WebApplicationException("Your request is missing data!", 400);
        }

        validateComment(content);

        content = content.trim();

        MongoDatabase database = application.getDatabase("pistonpost");
        MongoCollection<PostStorage> collection = database.getCollection("posts", PostStorage.class);

        Bson query = eq("postId", postId);
        PostStorage post = collection.find(query).first();

        if (post == null) {
            throw new WebApplicationException("Post not found!", 404);
        }

        List<ObjectId> commentStorageList = post.getComments();

        if (commentStorageList == null) {
            commentStorageList = new ArrayList<>();
        }

        ObjectId commentId = new ObjectId();

        CommentStorage commentStorage = new CommentStorage(commentId, content, user.getId());

        MongoCollection<CommentStorage> commentCollection = database.getCollection("comments", CommentStorage.class);

        commentCollection.insertOne(commentStorage);

        commentStorageList.add(commentId);
        post.setComments(commentStorageList);

        collection.replaceOne(query, post);
    }

    @DELETE
    @Path("/{postId}/comment/{commentId}")
    @Operation(
            summary = "Delete a comment of a post",
            description = "Delete a comment of a post.",
            tags = {"post"}
    )
    public void commentDelete(@Parameter(hidden = true) @Auth User user, @PathParam("postId") String postId, @PathParam("commentId") String commentId) {
        MongoDatabase database = application.getDatabase("pistonpost");

        MongoCollection<CommentStorage> commentCollection = database.getCollection("comments", CommentStorage.class);

        ObjectId commentObjectId = new ObjectId(commentId);
        Bson commentQuery = eq("_id", commentObjectId);
        CommentStorage comment = commentCollection.find(commentQuery).first();

        if (comment == null) {
            throw new WebApplicationException("Comment not found!", 404);
        }

        if (!user.getRoles().contains("ADMIN")
                && !comment.getAuthor().equals(user.getId())) {
            throw new WebApplicationException("You can only delete your own comments!", 403);
        }

        commentCollection.deleteOne(commentQuery);

        MongoCollection<PostStorage> collection = database.getCollection("posts", PostStorage.class);

        Bson postQuery = eq("postId", postId);
        PostStorage post = collection.find(postQuery).first();

        if (post == null) {
            throw new WebApplicationException("Post not found!", 404);
        }

        List<ObjectId> commentStorageList = post.getComments();

        if (commentStorageList == null) {
            throw new WebApplicationException("Comment not found!", 404);
        }

        commentStorageList.remove(commentObjectId);

        collection.replaceOne(postQuery, post);
    }

    @PUT
    @Path("/{postId}/vote")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Put a vote for a post",
            description = "Put a vote for a post.",
            tags = {"post"}
    )
    public void putVote(@Parameter(hidden = true) @Auth User user, @PathParam("postId") String postId, @QueryParam("type") VoteType type) {
        if (type == null) {
            throw new WebApplicationException("Your request is missing data!", 400);
        }

        MongoDatabase database = application.getDatabase("pistonpost");
        MongoCollection<PostStorage> collection = database.getCollection("posts", PostStorage.class);

        Bson query = eq("postId", postId);
        PostStorage post = collection.find(query).first();

        if (post == null) {
            throw new WebApplicationException("Post not found!", 404);
        }

        Set<ObjectId> storageList = switch (type) {
            case LIKE -> post.getLikes();
            case DISLIKE -> post.getDislikes();
            case HEART -> post.getHearts();
        };

        if (storageList == null) {
            storageList = new HashSet<>();
        }
        storageList.add(user.getId());

        switch (type) {
            case LIKE -> post.setLikes(storageList);
            case DISLIKE -> post.setDislikes(storageList);
            case HEART -> post.setHearts(storageList);
        }

        collection.replaceOne(query, post);
    }

    @DELETE
    @Path("/{postId}/vote")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Remove a vote of a post",
            description = "Remove a vote of a post.",
            tags = {"post"}
    )
    public void deleteVote(@Parameter(hidden = true) @Auth User user, @PathParam("postId") String postId, @QueryParam("type") VoteType type) {
        if (type == null) {
            throw new WebApplicationException("Your request is missing data!", 400);
        }

        MongoDatabase database = application.getDatabase("pistonpost");
        MongoCollection<PostStorage> collection = database.getCollection("posts", PostStorage.class);

        Bson query = eq("postId", postId);
        PostStorage post = collection.find(query).first();

        if (post == null) {
            throw new WebApplicationException("Post not found!", 404);
        }

        Set<ObjectId> storageList = switch (type) {
            case LIKE -> post.getLikes();
            case DISLIKE -> post.getDislikes();
            case HEART -> post.getHearts();
        };

        if (storageList == null) {
            storageList = new HashSet<>();
        }
        storageList.remove(user.getId());

        switch (type) {
            case LIKE -> post.setLikes(storageList);
            case DISLIKE -> post.setDislikes(storageList);
            case HEART -> post.setHearts(storageList);
        }

        collection.replaceOne(query, post);
    }
}
