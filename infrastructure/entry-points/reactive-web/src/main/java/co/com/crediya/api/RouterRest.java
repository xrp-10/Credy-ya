package co.com.crediya.api;

import co.com.crediya.api.auth.AuthHandler;
import co.com.crediya.api.solicitud.SolicitudHandler;
import co.com.crediya.api.usuario.UsuarioHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterRest {

    public static final String USUARIOS = "/api/v1/usuarios";
    public static final String SOLICITUD = "/api/v1/solicitud";
    public static final String LOGIN = "/api/v1/login";

    @Bean
    public RouterFunction<ServerResponse> usuarioRoutes(UsuarioHandler handler) {
        return route(POST(USUARIOS), handler::registrar);
    }

    @Bean
    public RouterFunction<ServerResponse> solicitudRoutes(SolicitudHandler handler) {
        return route(POST(SOLICITUD), handler::registrar);
    }

    @Bean
    public RouterFunction<ServerResponse> authRoutes(AuthHandler handler) {
        return route(POST(LOGIN), handler::login);
    }
}
