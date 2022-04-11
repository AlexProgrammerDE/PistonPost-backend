package net.pistonmaster.pistonpost.auth;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import net.pistonmaster.pistonpost.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class UserAuthenticator implements Authenticator<String, User> {
    private static final Logger LOG = LoggerFactory.getLogger(UserAuthenticator.class);

    @Override
    public Optional<User> authenticate(String token) throws AuthenticationException {
        LOG.info(token);
        return Optional.of(new User("NameHere"));
    }
}
