package net.pistonmaster.pistonpost.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.crypto.DirectDecrypter;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.jackson.Jackson;
import keywhiz.hkdf.Hkdf;
import net.pistonmaster.pistonpost.PistonPostApplication;
import net.pistonmaster.pistonpost.User;
import net.pistonmaster.pistonpost.storage.UserDataStorage;
import net.pistonmaster.pistonpost.utils.JWTToken;
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
    private final PistonPostApplication application;

    public UserAuthenticator(PistonPostApplication application, String jwtTokenSecret) {
        Hkdf hkdf = Hkdf.usingDefaults();
        SecretKey initialKey = hkdf.extract(null, jwtTokenSecret.getBytes(StandardCharsets.UTF_8));

        key = hkdf.expand(initialKey, "NextAuth.js Generated Encryption Key".getBytes(StandardCharsets.UTF_8), 32);
        this.application = application;
    }

    @Override
    public Optional<User> authenticate(String token) throws AuthenticationException {
        try {
            JWEObject jweObject = JWEObject.parse(token);

            jweObject.decrypt(new DirectDecrypter(key));

            JWTToken jwt = Jackson.newObjectMapper().readValue(jweObject.getPayload().toString(), JWTToken.class);

            MongoDatabase database = application.getDatabase("pistonpost");
            MongoCollection<UserDataStorage> collection = database.getCollection("users", UserDataStorage.class);

            Bson query = eq("_id", new ObjectId(jwt.getSub()));
            UserDataStorage storage = collection.find(query).first();

            if (storage != null) {
                if (storage.getName() == null) {
                    String potentialName;
                    do {
                        potentialName = application.getFaker().funnyName().name().replace(" ", "");
                    } while (potentialName.length() > 12);

                    storage.setName(potentialName);

                    collection.replaceOne(query, storage, new ReplaceOptions().upsert(false));
                }

                return Optional.of(new User(storage));
            }

            return Optional.empty();
        } catch (JOSEException | ParseException | JsonProcessingException e) {
            return Optional.empty();
        }
    }
}
