package net.pistonmaster.pistonpost.utils;

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

    public PostResponse fillPostStorage(ObjectId currentUser, PostStorage post, MongoDatabase database) {
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
                MongoCollection<ImageStorage> imageCollection = database.getCollection("images", ImageStorage.class);
                ImageStorage thumbnail = imageCollection.find(eq("_id", video.getThumbnailId())).first();
                if (thumbnail != null) {
                    videoResponse = new VideoResponse(video.getId().toHexString(), video.getExtension(), new ImageResponse(thumbnail.getId().toHexString(), thumbnail.getExtension(), thumbnail.getWidth(), thumbnail.getHeight()), video.getWidth(), video.getHeight());
                }
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

        if (post.getLikes() == null)
            post.setLikes(Set.of());

        if (post.getDislikes() == null)
            post.setDislikes(Set.of());

        if (post.getHearts() == null)
            post.setHearts(Set.of());

        System.out.println("Current user: " + currentUser);
        System.out.println("Post: " + post.getLikes());
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
                authorData,
                new VoteResponse(post.getLikes().size(), checkVoted(currentUser, post.getLikes())),
                new VoteResponse(post.getDislikes().size(), checkVoted(currentUser, post.getDislikes())),
                new VoteResponse(post.getHearts().size(), checkVoted(currentUser, post.getHearts()))
        );
    }

    private boolean checkVoted(ObjectId currentUser, Set<ObjectId> list) {
        return currentUser != null && list.stream().anyMatch(currentUser::equals);
    }

    public UserDataResponse fillUserDataStorage(MongoDatabase database, ObjectId user) {
        MongoCollection<UserDataStorage> collection = database.getCollection("users", UserDataStorage.class);

        Bson query = eq("_id", user);

        UserDataStorage userData = collection.find(query).first();
        return userData == null ?
                DELETED_ACCOUNT : new User(userData).generateUserDataResponse();
    }
}
