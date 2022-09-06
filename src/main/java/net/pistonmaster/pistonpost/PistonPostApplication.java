package net.pistonmaster.pistonpost;

import com.github.javafaker.Faker;
import com.mongodb.client.MongoDatabase;
import com.twelvemonkeys.servlet.image.IIOProviderContextListener;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.ResourceConfigurationSourceProvider;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.forms.MultiPartBundle;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.Getter;
import net.pistonmaster.pistonpost.auth.AuthCredentialFilter;
import net.pistonmaster.pistonpost.auth.UserAuthenticator;
import net.pistonmaster.pistonpost.auth.UserAuthorizer;
import net.pistonmaster.pistonpost.manager.StaticFileManager;
import net.pistonmaster.pistonpost.resources.*;
import net.pistonmaster.pistonpost.servlets.FileAssetServlet;
import net.pistonmaster.pistonpost.utils.PostFillerService;
import org.eclipse.jetty.servlets.DoSFilter;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

import javax.imageio.ImageIO;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public class PistonPostApplication extends Application<PistonPostConfiguration> {
    private final MongoManager mongoManager = new MongoManager();
    private final Faker faker = new Faker();
    private final PostFillerService postFillerService = new PostFillerService(this);

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
                new AuthCredentialFilter.Builder<User>()
                        .setAuthenticator(new UserAuthenticator(this, configuration.getJwtTokenSecret()))
                        .setAuthorizer(new UserAuthorizer())
                        .setPrefix("Bearer")
                        .buildAuthFilter()));
        environment.jersey().register(RolesAllowedDynamicFeature.class);
        // If you want to use @Auth to inject a custom Principal type into your resource
        environment.jersey().register(new AuthValueFactoryProvider.Binder<>(User.class));

        mongoManager.setConnectUri(configuration.getMongoDbUri());

        ImageIO.scanForPlugins();
        environment.servlets().addServletListeners(new IIOProviderContextListener());
        environment.servlets().addFilter("DoSFilter", new DoSFilter());

        environment.jersey().register(new HomeResource(this));
        environment.jersey().register(new TagResource(this));
        environment.jersey().register(new UserResource(this));
        environment.jersey().register(new SettingsResource(this));
        environment.jersey().register(new PostsResource(this));

        environment.servlets().addServlet("file-assets", new FileAssetServlet("static/", "/static/", null, StandardCharsets.UTF_8)).addMapping("/static/*");
        StaticFileManager staticFileManager = new StaticFileManager(configuration.getStaticFilesPath(), this);
        staticFileManager.init();
        environment.jersey().register(new PostResource(this, staticFileManager));

        OpenAPI oas = new OpenAPI();
        Info info = new Info()
                .title("PistonPost API")
                .version(configuration.getVersion())
                .description("Open source platform inspired by Reddit.")
                .termsOfService("https://post.pistonmaster.net/tos")
                .contact(new Contact().name("AlexProgrammerDE").url("https://pistonmaster.net"));

        oas.components(new Components().securitySchemes(
                Map.of(
                        "bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                )
        ));
        oas.security(List.of(new SecurityRequirement().addList("bearerAuth")));
        oas.info(info);
        oas.servers(List.of(new Server().url("https://post.pistonmaster.net/backend").description("Production backend")));

        SwaggerConfiguration oasConfig = new SwaggerConfiguration()
                .openAPI(oas)
                .prettyPrint(true)
                .resourcePackages(Stream.of("net.pistonmaster.pistonpost").collect(Collectors.toSet()));
        environment.jersey().register(new OpenApiResource().openApiConfiguration(oasConfig));
    }

    public MongoDatabase getDatabase(String databaseName) {
        return mongoManager.getDatabase(databaseName);
    }
}
