package co.com.crediya.api.error;

import co.com.crediya.model.exceptions.BusinessErrorType;
import co.com.crediya.model.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

/**
 * Convierte toda excepción en una respuesta JSON controlada, para que quien usa la API
 * nunca reciba trazas ni mensajes internos. Se ejecuta antes que el manejador de Spring (-1).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalErrorHandler implements WebExceptionHandler, Ordered {

    static final String ERROR_INTERNO = "Ocurrió un error inesperado. Intenta de nuevo más tarde";

    private static final int ANTES_QUE_SPRING = -2;

    private final ErrorResponseWriter writer;

    @Override
    public int getOrder() {
        return ANTES_QUE_SPRING;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        String ruta = exchange.getRequest().getPath().value();
        if (ex instanceof BusinessException negocio) {
            log.warn("Regla de negocio incumplida en {}: {}", ruta, negocio.getMessage());
            return writer.write(exchange, estadoDe(negocio.getType()), negocio.getType().name(), negocio.getMessage());
        }
        if (ex instanceof ServerWebInputException entrada) {
            log.warn("Petición mal formada en {}: {}", ruta, entrada.getReason());
            return writer.write(exchange, HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA",
                    "La petición no tiene un formato válido. Revisa los campos y sus tipos");
        }
        if (ex instanceof ResponseStatusException estado) {
            log.warn("Petición rechazada en {} con estado {}", ruta, estado.getStatusCode());
            return writer.write(exchange, estado.getStatusCode(), "SOLICITUD_RECHAZADA",
                    mensajeDe(estado.getStatusCode()));
        }
        log.error("Error no controlado en {}", ruta, ex);
        return writer.write(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO", ERROR_INTERNO);
    }

    static HttpStatus estadoDe(BusinessErrorType tipo) {
        return switch (tipo) {
            case INVALID_DATA -> HttpStatus.BAD_REQUEST;
            case ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
        };
    }

    private static String mensajeDe(HttpStatusCode estado) {
        if (estado.value() == HttpStatus.NOT_FOUND.value()) {
            return "El recurso solicitado no existe";
        }
        if (estado.value() == HttpStatus.METHOD_NOT_ALLOWED.value()) {
            return "Método HTTP no permitido para este recurso";
        }
        if (estado.value() == HttpStatus.UNSUPPORTED_MEDIA_TYPE.value()) {
            return "Tipo de contenido no soportado, usa application/json";
        }
        return "La petición no se pudo procesar";
    }
}
