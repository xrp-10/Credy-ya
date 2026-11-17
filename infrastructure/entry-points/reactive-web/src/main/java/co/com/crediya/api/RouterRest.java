package co.com.crediya.api;

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

    @Bean
    public RouterFunction<ServerResponse> routerFunction(UsuarioHandler usuarioHandler) {
        return route(POST(USUARIOS), usuarioHandler::registrar);
    }
}
