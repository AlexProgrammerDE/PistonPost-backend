package net.pistonmaster.pistonpost;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.pistonpost.api.UserDataResponse;
import net.pistonmaster.pistonpost.storage.UserDataStorage;
import net.pistonmaster.pistonpost.utils.MD5Util;
import org.bson.types.ObjectId;

import java.security.Principal;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public class User implements Principal {
    private final ObjectId id;
    private final String name;
    private final String avatar;
    private final Set<String> roles;

    public User(ObjectId id, String name, String email) {
        this(
                id,
                name,
                generateAvatar(email),
                Set.of()
        );
    }

    public User(UserDataStorage userDataStorage) {
        this(
                userDataStorage.getId(),
                userDataStorage.getName(),
                userDataStorage.getEmail()
        );
    }

    private static String generateAvatar(String email) {
        return String.format(
                "https://www.gravatar.com/avatar/%s?d=retro",
                MD5Util.md5Hex(email.toLowerCase())
        );
    }

    public UserDataResponse generateUserDataResponse() {
        return new UserDataResponse(name, avatar);
    }
}
