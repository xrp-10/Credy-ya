package co.com.crediya.api.usuario;

import co.com.crediya.model.exceptions.BusinessErrorType;
import co.com.crediya.model.exceptions.BusinessException;
import co.com.crediya.usecase.usuario.UsuarioUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;

@Slf4j
@Component
@RequiredArgsConstructor
public class UsuarioHandler {

    private final UsuarioUseCase usuarioUseCase;

    public Mono<ServerResponse> registrar(ServerRequest request) {
        return request.bodyToMono(UsuarioRequest.class)
                .switchIfEmpty(Mono.error(new BusinessException(BusinessErrorType.INVALID_DATA,
                        "El cuerpo de la petición es obligatorio")))
                .doOnNext(body -> log.info("Registro de usuario recibido"))
                .map(UsuarioRequest::toDomain)
                .flatMap(usuarioUseCase::registrar)
                .doOnNext(usuario -> log.info("Usuario registrado con id {} y rol {}",
                        usuario.getIdUsuario(), usuario.getIdRol()))
                .flatMap(usuario -> ServerResponse
                        .created(URI.create(request.path() + "/" + usuario.getIdUsuario()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(UsuarioResponse.from(usuario)));
    }
}
