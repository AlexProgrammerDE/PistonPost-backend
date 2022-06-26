package net.pistonmaster.pistonpost.utils;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.pistonmaster.pistonpost.PistonPostApplication;
import net.pistonmaster.pistonpost.User;
import net.pistonmaster.pistonpost.api.*;
import net.pistonmaster.pistonpost.storage.*;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.mongodb.client.model.Filters.eq;

public record PostFillerService(PistonPostApplication application) {
    public static final UserDataResponse DELETED_ACCOUNT = new UserDataResponse(
            new ObjectId().toHexString(),
            "Deleted Account",
            "https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y",
            Set.of()
    );

    public PostResponse fillPostStorage(PostStorage post) {
        try (MongoClient mongoClient = application.createClient()) {
            MongoDatabase database = mongoClient.getDatabase("pistonpost");
            UserDataResponse authorData = fillUserDataStorage(database, post.getAuthor());

            List<ImageResponse> imageResponse = null;
            VideoResponse videoResponse = null;

            if (post.getImageIds() != null) {
                imageResponse = new ArrayList<>();
                List<ImageStorage> imageStorage = new ArrayList<>();
                MongoCollection<ImageStorage> imageCollection = database.getCollection("images", ImageStorage.class);
                for (ObjectId imageId : post.getImageIds()) {
                    imageStorage.add(imageCollection.find(eq("_id", imageId)).first());
                }
                for (ImageStorage image : imageStorage) {
                    if (image != null) {
                        imageResponse.add(new ImageResponse(image.getId().toHexString(), image.getExtension(), image.getWidth(), image.getHeight()));
                    }
                }
            }

            if (post.getVideoId() != null) {
                MongoCollection<VideoStorage> videoCollection = database.getCollection("videos", VideoStorage.class);
                VideoStorage video = videoCollection.find(eq("_id", post.getVideoId())).first();
                if (video != null) {
                    videoResponse = new VideoResponse(video.getId().toHexString(), video.getExtension(), video.getThumbnailId());
                }
            }

            PostType postType = post.getType();

            if (postType == null && post.getContent() != null) {
                postType = PostType.TEXT;
            }

            List<CommentResponse> commentResponse = new ArrayList<>();

            if (post.getComments() != null) {
                MongoCollection<CommentStorage> commentCollection = database.getCollection("comments", CommentStorage.class);
                for (ObjectId commentId : post.getComments()) {
                    CommentStorage comment = commentCollection.find(eq("_id", commentId)).first();
                    if (comment != null) {
                        commentResponse.add(new CommentResponse(comment.getId().toHexString(), comment.getContent(), fillUserDataStorage(database, comment.getAuthor())));
                    }
                }
            }

            return new PostResponse(
                    post.getPostId(),
                    post.getTitle(),
                    postType,
                    post.getContent(),
                    imageResponse,
                    videoResponse,
                    post.getTags(),
                    commentResponse,
                    post.getTimestamp(),
                    post.isUnlisted(),
                    authorData
            );
        }
    }

    public UserDataResponse fillUserDataStorage(MongoDatabase database, ObjectId user) {
        MongoCollection<UserDataStorage> collection = database.getCollection("users", UserDataStorage.class);

        Bson query = eq("_id", user);

        UserDataStorage userData = collection.find(query).first();
        return userData == null ?
                DELETED_ACCOUNT : new User(userData).generateUserDataResponse();
    }
}
