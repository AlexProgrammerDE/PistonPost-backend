package net.pistonmaster.pistonpost;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.core.Configuration;
import jakarta.validation.constraints.NotEmpty;

public class PistonPostConfiguration extends Configuration {
    @NotEmpty
    private String jwtTokenSecret;

    @NotEmpty
    private String mongoDbUri;

    @NotEmpty
    private String version;

    @NotEmpty
    private String staticFilesPath;

    @JsonProperty
    public String getJwtTokenSecret() {
        return jwtTokenSecret;
    }

    @JsonProperty
    public String getMongoDbUri() {
        return mongoDbUri;
    }

    @JsonProperty
    public String getVersion() {
        return version;
    }

    @JsonProperty
    public String getStaticFilesPath() {
        return staticFilesPath;
    }
}
