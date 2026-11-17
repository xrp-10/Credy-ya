package co.com.crediya.api.solicitud;

import co.com.crediya.model.exceptions.BusinessErrorType;
import co.com.crediya.model.exceptions.BusinessException;
import co.com.crediya.usecase.solicitud.SolicitudUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.security.Principal;

@Slf4j
@Component
@RequiredArgsConstructor
public class SolicitudHandler {

    private final SolicitudUseCase solicitudUseCase;

    /** El correo de quien llama sale del token, nunca del cuerpo de la petición. */
    public Mono<ServerResponse> registrar(ServerRequest request) {
        return request.principal()
                .map(Principal::getName)
                .switchIfEmpty(Mono.error(new BusinessException(BusinessErrorType.UNAUTHORIZED,
                        "Debes iniciar sesión para crear una solicitud")))
                .flatMap(emailSolicitante -> request.bodyToMono(SolicitudRequest.class)
                        .switchIfEmpty(Mono.error(new BusinessException(BusinessErrorType.INVALID_DATA,
                                "El cuerpo de la petición es obligatorio")))
                        .doOnNext(body -> log.info("Solicitud de préstamo recibida para el tipo {}", body.idTipoPrestamo()))
                        .flatMap(body -> solicitudUseCase.registrar(body.toDomain(), body.documentoIdentidad(),
                                emailSolicitante)))
                .doOnNext(solicitud -> log.info("Solicitud {} registrada en estado {}",
                        solicitud.getIdSolicitud(), solicitud.getIdEstado()))
                .flatMap(solicitud -> ServerResponse
                        .created(URI.create(request.path() + "/" + solicitud.getIdSolicitud()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(SolicitudResponse.from(solicitud)));
    }
}
