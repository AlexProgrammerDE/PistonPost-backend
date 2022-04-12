package net.pistonmaster.pistonpost.resources;

import com.codahale.metrics.annotation.Timed;
import io.dropwizard.auth.Auth;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import net.pistonmaster.pistonpost.User;
import net.pistonmaster.pistonpost.api.UserDataResponse;

@Path("/userdata")
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {
    @GET
    @Timed
    public UserDataResponse userData(@Auth User user) {
        return user.generateUserDataResponse();
    }
}
