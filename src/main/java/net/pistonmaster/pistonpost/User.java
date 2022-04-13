package net.pistonmaster.pistonpost;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.pistonpost.api.UserDataResponse;
import net.pistonmaster.pistonpost.storage.UserDataStorage;
import net.pistonmaster.pistonpost.utils.MD5Util;

import java.security.Principal;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public class User implements Principal {
    private final String id;
    private final String name;
    private final String avatar;
    private final Set<String> roles;

    public User(String id, String name, String email) {
        this(
                id,
                name,
                generateAvatar(email),
                Set.of()
        );
    }

    public User(UserDataStorage userDataStorage) {
        this(
                userDataStorage.getId().toHexString(),
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
