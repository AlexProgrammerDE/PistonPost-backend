package net.pistonmaster.pistonpost.auth;

import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.Authenticator;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.SecurityContext;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.security.Principal;

@Priority(Priorities.AUTHENTICATION)
public class AuthCredentialFilter<P extends Principal> extends AuthFilter<String, P> {
    public static final String OAUTH_ACCESS_TOKEN_PARAM = "access_token";
    public static final String NEXTJS_TOKEN_COOKIE = "__Secure-next-auth.session-token";
    public static final String NEXTJS_TOKEN_COOKIE2 = "next-auth.session-token";

    private AuthCredentialFilter() {
    }

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        String credentials = getCredentials(requestContext.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));

        // If Authorization header is not used, check query parameter where token can be passed as well
        if (credentials == null) {
            credentials = requestContext.getUriInfo().getQueryParameters().getFirst(OAUTH_ACCESS_TOKEN_PARAM);
        }
        if (credentials == null) {
            Cookie cookie = requestContext.getCookies().get(NEXTJS_TOKEN_COOKIE);
            if (cookie != null) {
                credentials = cookie.getValue();
            }
        }
        if (credentials == null) {
            Cookie cookie = requestContext.getCookies().get(NEXTJS_TOKEN_COOKIE2);
            if (cookie != null) {
                credentials = cookie.getValue();
            }
        }

        if (!authenticate(requestContext, credentials, SecurityContext.BASIC_AUTH)) {
            throw unauthorizedHandler.buildException(prefix, realm);
        }
    }

    @Nullable
    private String getCredentials(String header) {
        if (header == null) {
            return null;
        }

        final int space = header.indexOf(' ');
        if (space <= 0) {
            return null;
        }

        final String method = header.substring(0, space);
        if (!prefix.equalsIgnoreCase(method)) {
            return null;
        }

        return header.substring(space + 1);
    }

    public static class Builder<P extends Principal>
            extends AuthFilterBuilder<String, P, AuthCredentialFilter<P>> {

        @Override
        protected AuthCredentialFilter<P> newInstance() {
            return new AuthCredentialFilter<>();
        }
    }
}
