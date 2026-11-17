package co.com.crediya.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.net.URI;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/** Documentación Swagger de la API. La interfaz queda en /swagger-ui.html y la raíz redirige a ella. */
@Configuration
public class OpenApiConfig {

    public static final String BEARER = "bearer";
    public static final String SWAGGER_UI = "/swagger-ui.html";

    @Bean
    public OpenAPI crediYaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("CrediYa API")
                        .version("v1")
                        .description("Solicitudes de préstamo (HU1 a HU4). Inicia sesión en POST /api/v1/login, "
                                + "copia el token y pégalo en el botón Authorize."))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }

    @Bean
    public RouterFunction<ServerResponse> inicioRoutes() {
        return route(GET("/"), request -> ServerResponse.temporaryRedirect(URI.create(SWAGGER_UI)).build());
    }
}
