package co.com.crediya.api.error;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/** Escribe un {@link ErrorResponse} en JSON con los mismos codecs que usa el resto de la API. */
@Component
@RequiredArgsConstructor
public class ErrorResponseWriter {

    private final ServerCodecConfigurer codecConfigurer;

    public Mono<Void> write(ServerWebExchange exchange, HttpStatusCode status, String error, String mensaje) {
        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ErrorResponse(status.value(), error, mensaje))
                .flatMap(response -> response.writeTo(exchange, new Contexto(codecConfigurer.getWriters())));
    }

    private record Contexto(List<HttpMessageWriter<?>> messageWriters) implements ServerResponse.Context {
        @Override
        public List<ViewResolver> viewResolvers() {
            return List.of();
        }
    }
}
