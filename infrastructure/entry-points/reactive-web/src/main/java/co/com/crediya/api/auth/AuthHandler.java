package co.com.crediya.api.auth;

import co.com.crediya.model.exceptions.BusinessErrorType;
import co.com.crediya.model.exceptions.BusinessException;
import co.com.crediya.usecase.login.LoginUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthHandler {

    private final LoginUseCase loginUseCase;

    public Mono<ServerResponse> login(ServerRequest request) {
        return request.bodyToMono(LoginRequest.class)
                .switchIfEmpty(Mono.error(new BusinessException(BusinessErrorType.INVALID_DATA,
                        "El cuerpo de la petición es obligatorio")))
                .doOnNext(body -> log.info("Intento de inicio de sesión recibido"))
                .flatMap(body -> loginUseCase.login(body.correoElectronico(), body.clave()))
                .doOnNext(token -> log.info("Inicio de sesión exitoso"))
                .flatMap(token -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(TokenResponse.bearer(token)));
    }
}
