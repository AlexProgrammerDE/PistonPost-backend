package net.pistonmaster.pistonpost;

import com.github.javafaker.Faker;
import com.mongodb.client.MongoClient;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.ResourceConfigurationSourceProvider;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.forms.MultiPartBundle;
import lombok.Getter;
import net.pistonmaster.pistonpost.api.SettingsResponse;
import net.pistonmaster.pistonpost.auth.AdminAuthorizer;
import net.pistonmaster.pistonpost.auth.UserAuthenticator;
import net.pistonmaster.pistonpost.resources.SettingsResource;
import net.pistonmaster.pistonpost.resources.UserResource;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

@Getter
public class PistonPostApplication extends Application<PistonPostConfiguration> {
    private final MongoManager mongoManager = new MongoManager();
    private final Faker faker = new Faker();

    public static void main(String[] args) throws Exception {
        new PistonPostApplication().run("server", "/config.yml");
    }

    @Override
    public String getName() {
        return "PistonPost";
    }

    @Override
    public void initialize(Bootstrap<PistonPostConfiguration> bootstrap) {
        bootstrap.addBundle(new MultiPartBundle());

        bootstrap.setConfigurationSourceProvider(new ResourceConfigurationSourceProvider());

        // Enable variable substitution with environment variables
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)
                )
        );
    }

    @Override
    public void run(PistonPostConfiguration configuration,
                    Environment environment) {
        environment.healthChecks().register("MongoDB", mongoManager);

        environment.jersey().register(new AuthDynamicFeature(
                new OAuthCredentialAuthFilter.Builder<User>()
                        .setAuthenticator(new UserAuthenticator(this, configuration.getJwtTokenSecret()))
                        .setAuthorizer(new AdminAuthorizer())
                        .setPrefix("Bearer")
                        .buildAuthFilter()));
        environment.jersey().register(RolesAllowedDynamicFeature.class);
        // If you want to use @Auth to inject a custom Principal type into your resource
        environment.jersey().register(new AuthValueFactoryProvider.Binder<>(User.class));

        mongoManager.setConnectUri(configuration.getMongoDbUri());

        environment.jersey().register(new UserResource());
        environment.jersey().register(new SettingsResource(this));
    }

    public MongoClient createClient() {
        return mongoManager.createClient();
    }
}
