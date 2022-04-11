package net.pistonmaster.pistonpost.auth;

import io.dropwizard.auth.Authorizer;
import jakarta.ws.rs.container.ContainerRequestContext;
import net.pistonmaster.pistonpost.User;

import javax.annotation.Nullable;

public class AdminAuthorizer implements Authorizer<User> {
    @Override
    public boolean authorize(User user, String role, @Nullable ContainerRequestContext requestContext) {
        return role.equals("ADMIN");
    }
}
