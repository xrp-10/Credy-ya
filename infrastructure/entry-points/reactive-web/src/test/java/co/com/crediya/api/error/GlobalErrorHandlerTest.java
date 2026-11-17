package co.com.crediya.api.error;

import co.com.crediya.model.exceptions.BusinessErrorType;
import co.com.crediya.model.exceptions.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;

class GlobalErrorHandlerTest {

    private final GlobalErrorHandler handler =
            new GlobalErrorHandler(new ErrorResponseWriter(ServerCodecConfigurer.create()));

    private final RouterFunction<ServerResponse> rutas = RouterFunctions
            .route(GET("/negocio"), r -> Mono.error(new BusinessException(BusinessErrorType.INVALID_DATA, "dato malo")))
            .andRoute(GET("/no-encontrado"), r -> Mono.error(new BusinessException(BusinessErrorType.NOT_FOUND, "no está")))
            .andRoute(GET("/sin-sesion"), r -> Mono.error(new BusinessException(BusinessErrorType.UNAUTHORIZED, "sin sesión")))
            .andRoute(GET("/prohibido"), r -> Mono.error(new BusinessException(BusinessErrorType.FORBIDDEN, "no puedes")))
            .andRoute(GET("/duplicado"), r -> Mono.error(new BusinessException(BusinessErrorType.ALREADY_EXISTS, "ya existe")))
            .andRoute(GET("/rechazado"), r -> Mono.error(new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED)))
            .andRoute(GET("/tipo"), r -> Mono.error(new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE)))
            .andRoute(GET("/conflicto"), r -> Mono.error(new ResponseStatusException(HttpStatus.CONFLICT)))
            .andRoute(GET("/interno"), r -> Mono.error(new IllegalStateException("detalle interno secreto")))
            .andRoute(POST("/json"), r -> r.bodyToMono(Map.class).flatMap(b -> ServerResponse.ok().build()));

    private final WebTestClient client = WebTestClient.bindToRouterFunction(rutas)
            .handlerStrategies(HandlerStrategies.empty()
                        .codecs(codecs -> codecs.registerDefaults(true))
                        .exceptionHandler(handler).build())
            .build();

    @Test
    void erroresDeNegocioDevuelvenSuEstadoYMensaje() {
        client.get().uri("/negocio").exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.estado").isEqualTo(400)
                .jsonPath("$.error").isEqualTo("INVALID_DATA")
                .jsonPath("$.mensaje").isEqualTo("dato malo");
        client.get().uri("/duplicado").exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody().jsonPath("$.error").isEqualTo("ALREADY_EXISTS");
        client.get().uri("/sin-sesion").exchange()
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.error").isEqualTo("UNAUTHORIZED");
        client.get().uri("/prohibido").exchange()
                .expectStatus().isForbidden()
                .expectBody().jsonPath("$.error").isEqualTo("FORBIDDEN");
        client.get().uri("/no-encontrado").exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.error").isEqualTo("NOT_FOUND");
    }

    @Test
    void jsonMalFormadoDevuelve400() {
        client.post().uri("/json").contentType(MediaType.APPLICATION_JSON).bodyValue("{malo")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.error").isEqualTo("SOLICITUD_INVALIDA");
    }

    @Test
    void rutaInexistenteYOtrosEstadosTienenMensajeControlado() {
        client.get().uri("/no-existe").exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.mensaje").isEqualTo("El recurso solicitado no existe");
        client.get().uri("/rechazado").exchange()
                .expectStatus().isEqualTo(HttpStatus.METHOD_NOT_ALLOWED)
                .expectBody().jsonPath("$.mensaje").isEqualTo("Método HTTP no permitido para este recurso");
        client.get().uri("/tipo").exchange()
                .expectStatus().isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .expectBody().jsonPath("$.mensaje").isEqualTo("Tipo de contenido no soportado, usa application/json");
        client.get().uri("/conflicto").exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody().jsonPath("$.mensaje").isEqualTo("La petición no se pudo procesar");
    }

    @Test
    void errorInesperadoNoExponeDetalles() {
        client.get().uri("/interno").exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody(String.class)
                .value(cuerpo -> assertThat(cuerpo)
                        .contains(GlobalErrorHandler.ERROR_INTERNO)
                        .doesNotContain("detalle interno secreto"));
    }
}
