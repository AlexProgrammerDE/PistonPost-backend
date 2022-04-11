package net.pistonmaster.pistonpost.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.javafaker.Faker;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.result.UpdateResult;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.crypto.DirectDecrypter;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.jackson.Jackson;
import keywhiz.hkdf.Hkdf;
import net.pistonmaster.pistonpost.MongoManager;
import net.pistonmaster.pistonpost.User;
import net.pistonmaster.pistonpost.api.JWTToken;
import net.pistonmaster.pistonpost.api.UserDataResponse;
import net.pistonmaster.pistonpost.api.UserDataStorage;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;

public class UserAuthenticator implements Authenticator<String, User> {
    private static final Logger LOG = LoggerFactory.getLogger(UserAuthenticator.class);
    private final byte[] key;
    private final MongoManager mongoManager;

    public UserAuthenticator(MongoManager mongoManager, String jwtTokenSecret) {
        Hkdf hkdf = Hkdf.usingDefaults();
        SecretKey initialKey = hkdf.extract(null, jwtTokenSecret.getBytes(StandardCharsets.UTF_8));

        key = hkdf.expand(initialKey, "NextAuth.js Generated Encryption Key".getBytes(StandardCharsets.UTF_8), 32);
        this.mongoManager = mongoManager;
    }

    @Override
    public Optional<User> authenticate(String token) throws AuthenticationException {
        try {
            JWEObject jweObject = JWEObject.parse(token);

            jweObject.decrypt(new DirectDecrypter(key));

            JWTToken jwt = Jackson.newObjectMapper().readValue(jweObject.getPayload().toString(), JWTToken.class);

            try (MongoClient mongoClient = mongoManager.createClient()) {
                MongoDatabase database = mongoClient.getDatabase("pistonpost");
                MongoCollection<UserDataStorage> collection = database.getCollection("users", UserDataStorage.class);

                Bson query = eq("_id", new ObjectId(jwt.getSub()));
                UserDataStorage storage = collection.find(query).first();

                if (storage != null) {
                    if (storage.getName() == null) {
                        Faker faker = new Faker();

                        String potentialName;
                        do {
                            potentialName = faker.funnyName().name().replace(" ", "");
                        } while (potentialName.length() > 12);

                        storage.setName(potentialName);

                        collection.replaceOne(query, storage, new ReplaceOptions().upsert(false));
                    }

                    return Optional.of(new User(jwt.getSub(), storage.getName(), storage.getEmail()));
                }
            }
        } catch (JOSEException | ParseException | JsonProcessingException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }
}
