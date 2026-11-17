package co.com.crediya.api.revision;

import co.com.crediya.model.exceptions.BusinessErrorType;
import co.com.crediya.model.exceptions.BusinessException;
import co.com.crediya.usecase.listarsolicitudes.ListarSolicitudesUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * GET /api/v1/solicitud?estado=&email=&tipo_prestamo=&pagina=&tamano=
 * Todos los parámetros son opcionales.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RevisionHandler {

    private final ListarSolicitudesUseCase listarSolicitudesUseCase;

    public Mono<ServerResponse> listar(ServerRequest request) {
        return Mono.fromCallable(() -> Parametros.de(request))
                .flatMap(p -> listarSolicitudesUseCase.listar(p.estado(), p.email(), p.tipoPrestamo(),
                        p.pagina(), p.tamano()))
                .doOnNext(pagina -> log.info("Listado de revisión: página {} con {} de {} solicitudes",
                        pagina.pagina(), pagina.contenido().size(), pagina.totalElementos()))
                .flatMap(pagina -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(PaginaResponse.from(pagina)));
    }

    private record Parametros(Integer estado, String email, Long tipoPrestamo, Integer pagina, Integer tamano) {

        static Parametros de(ServerRequest request) {
            return new Parametros(
                    numero(request, "estado", Integer::valueOf),
                    request.queryParam("email").orElse(null),
                    numero(request, "tipo_prestamo", Long::valueOf),
                    numero(request, "pagina", Integer::valueOf),
                    numero(request, "tamano", Integer::valueOf));
        }

        private static <T> T numero(ServerRequest request, String nombre, Function<String, T> convertir) {
            return request.queryParam(nombre)
                    .filter(valor -> !valor.isBlank())
                    .map(valor -> {
                        try {
                            return convertir.apply(valor.trim());
                        } catch (NumberFormatException e) {
                            throw new BusinessException(BusinessErrorType.INVALID_DATA,
                                    "El parámetro " + nombre + " debe ser un número entero");
                        }
                    })
                    .orElse(null);
        }
    }
}
