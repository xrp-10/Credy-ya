package co.com.crediya.api.solicitud;

import co.com.crediya.api.RouterRest;
import co.com.crediya.api.error.ErrorResponseWriter;
import co.com.crediya.api.error.GlobalErrorHandler;
import co.com.crediya.model.exceptions.BusinessErrorType;
import co.com.crediya.model.exceptions.BusinessException;
import co.com.crediya.model.solicitud.Solicitud;
import co.com.crediya.usecase.solicitud.SolicitudUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitudHandlerTest {

    private static final String CUERPO = """
            {"documento_identidad": "900000001", "monto": 5000000, "plazo": 24, "id_tipo_prestamo": 1}
            """;

    @Mock
    private SolicitudUseCase solicitudUseCase;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        GlobalErrorHandler errores = new GlobalErrorHandler(new ErrorResponseWriter(ServerCodecConfigurer.create()));
        client = WebTestClient.bindToRouterFunction(new RouterRest().solicitudRoutes(new SolicitudHandler(solicitudUseCase)))
                .webFilter((exchange, chain) -> chain.filter(exchange.mutate()
                        .principal(Mono.just((Principal) () -> "cliente@crediya.test")).build()))
                .handlerStrategies(HandlerStrategies.empty()
                        .codecs(codecs -> codecs.registerDefaults(true))
                        .exceptionHandler(errores).build())
                .build();
    }

    @Test
    void registraLaSolicitudYDevuelve201ConEstadoPendiente() {
        when(solicitudUseCase.registrar(any(), eq("900000001"), eq("cliente@crediya.test"))).thenAnswer(i -> Mono.just(((Solicitud) i.getArgument(0))
                .toBuilder().idSolicitud(10L).email("cliente@crediya.test").idEstado(1).build()));

        client.post().uri(RouterRest.SOLICITUD)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(CUERPO)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().location("/api/v1/solicitud/10")
                .expectBody()
                .jsonPath("$.id_solicitud").isEqualTo(10)
                .jsonPath("$.monto").isEqualTo(5000000)
                .jsonPath("$.plazo").isEqualTo(24)
                .jsonPath("$.email").isEqualTo("cliente@crediya.test")
                .jsonPath("$.id_tipo_prestamo").isEqualTo(1)
                .jsonPath("$.estado").isEqualTo("Pendiente de revisión");

        ArgumentCaptor<Solicitud> enviada = ArgumentCaptor.forClass(Solicitud.class);
        verify(solicitudUseCase).registrar(enviada.capture(), eq("900000001"), eq("cliente@crediya.test"));
        assertThat(enviada.getValue().getMonto()).isEqualByComparingTo(new BigDecimal("5000000"));
        assertThat(enviada.getValue().getPlazo()).isEqualTo(24);
        assertThat(enviada.getValue().getIdTipoPrestamo()).isEqualTo(1L);
    }

    @Test
    void erroresDelCasoDeUsoYCuerpoVacioSeRespondenEnJson() {
        when(solicitudUseCase.registrar(any(), any(), any())).thenReturn(Mono.error(
                new BusinessException(BusinessErrorType.NOT_FOUND, "No existe un cliente con ese documento de identidad")));

        client.post().uri(RouterRest.SOLICITUD).contentType(MediaType.APPLICATION_JSON).bodyValue(CUERPO)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.error").isEqualTo("NOT_FOUND");
        client.post().uri(RouterRest.SOLICITUD).contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.mensaje").isEqualTo("El cuerpo de la petición es obligatorio");
    }

    @Test
    void sinSesionResponde401() {
        GlobalErrorHandler errores = new GlobalErrorHandler(new ErrorResponseWriter(ServerCodecConfigurer.create()));
        WebTestClient anonimo = WebTestClient
                .bindToRouterFunction(new RouterRest().solicitudRoutes(new SolicitudHandler(solicitudUseCase)))
                .handlerStrategies(HandlerStrategies.empty()
                        .codecs(codecs -> codecs.registerDefaults(true))
                        .exceptionHandler(errores).build())
                .build();

        anonimo.post().uri(RouterRest.SOLICITUD).contentType(MediaType.APPLICATION_JSON).bodyValue(CUERPO)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.error").isEqualTo("UNAUTHORIZED");
    }
}
