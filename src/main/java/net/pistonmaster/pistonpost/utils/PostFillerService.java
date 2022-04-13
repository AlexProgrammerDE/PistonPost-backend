package net.pistonmaster.pistonpost.utils;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.pistonmaster.pistonpost.PistonPostApplication;
import net.pistonmaster.pistonpost.User;
import net.pistonmaster.pistonpost.api.PostResponse;
import net.pistonmaster.pistonpost.api.UserDataResponse;
import net.pistonmaster.pistonpost.storage.PostStorage;
import net.pistonmaster.pistonpost.storage.UserDataStorage;
import org.bson.conversions.Bson;

import static com.mongodb.client.model.Filters.eq;

public record PostFillerService(PistonPostApplication application) {
    public static final UserDataResponse DELETED_ACCOUNT = new UserDataResponse(
            "Deleted Account",
            "https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y"
    );

    public PostResponse fillPostStorage(PostStorage post) {
        try (MongoClient mongoClient = application.createClient()) {
            MongoDatabase database = mongoClient.getDatabase("pistonpost");
            MongoCollection<UserDataStorage> collection = database.getCollection("users", UserDataStorage.class);

            Bson query = eq("_id", post.getAuthor());

            UserDataStorage userData = collection.find(query).first();
            UserDataResponse userDataResponse = userData == null ?
                    DELETED_ACCOUNT : new User(userData).generateUserDataResponse();

            return new PostResponse(
                    post.getPostId(),
                    post.getTitle(),
                    post.getContent(),
                    post.getTags(),
                    post.getTimestamp(),
                    post.isUnlisted(),
                    userDataResponse
            );
        }
    }
}
