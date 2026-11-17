package co.com.crediya.api.revision;

import co.com.crediya.api.RouterRest;
import co.com.crediya.api.error.ErrorResponseWriter;
import co.com.crediya.api.error.GlobalErrorHandler;
import co.com.crediya.model.pagina.Pagina;
import co.com.crediya.model.solicitudrevision.SolicitudRevision;
import co.com.crediya.usecase.listarsolicitudes.ListarSolicitudesUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevisionHandlerTest {

    @Mock
    private ListarSolicitudesUseCase listarSolicitudesUseCase;

    private WebTestClient client;

    private final SolicitudRevision fila = SolicitudRevision.builder()
            .idSolicitud(2L).monto(new BigDecimal("8000000")).plazo(36).email("cliente@crediya.test")
            .nombre("Carlos Ruiz").tipoPrestamo("Libre inversión").tasaInteres(new BigDecimal("1.80"))
            .estadoSolicitud("Pendiente de revisión").salarioBase(new BigDecimal("4000000"))
            .deudaTotalMensualSolicitudesAprobadas(new BigDecimal("180745.33")).build();

    @BeforeEach
    void setUp() {
        GlobalErrorHandler errores = new GlobalErrorHandler(new ErrorResponseWriter(ServerCodecConfigurer.create()));
        client = WebTestClient.bindToRouterFunction(new RouterRest().revisionRoutes(new RevisionHandler(listarSolicitudesUseCase)))
                .handlerStrategies(HandlerStrategies.empty()
                        .codecs(codecs -> codecs.registerDefaults(true))
                        .exceptionHandler(errores).build())
                .build();
    }

    @Test
    void devuelveLaPaginaConLosCamposDeLaHistoria() {
        when(listarSolicitudesUseCase.listar(3, "cliente@crediya.test", 1L, 0, 5))
                .thenReturn(Mono.just(Pagina.de(List.of(fila), 0, 5, 1)));

        client.get().uri(RouterRest.SOLICITUD + "?estado=3&email=cliente@crediya.test&tipo_prestamo=1&pagina=0&tamano=5")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.pagina").isEqualTo(0)
                .jsonPath("$.tamano").isEqualTo(5)
                .jsonPath("$.total_elementos").isEqualTo(1)
                .jsonPath("$.total_paginas").isEqualTo(1)
                .jsonPath("$.contenido[0].monto").isEqualTo(8000000)
                .jsonPath("$.contenido[0].plazo").isEqualTo(36)
                .jsonPath("$.contenido[0].email").isEqualTo("cliente@crediya.test")
                .jsonPath("$.contenido[0].nombre").isEqualTo("Carlos Ruiz")
                .jsonPath("$.contenido[0].tipo_prestamo").isEqualTo("Libre inversión")
                .jsonPath("$.contenido[0].tasa_interes").isEqualTo(1.80)
                .jsonPath("$.contenido[0].estado_solicitud").isEqualTo("Pendiente de revisión")
                .jsonPath("$.contenido[0].salario_base").isEqualTo(4000000)
                .jsonPath("$.contenido[0].deuda_total_mensual_solicitudes_aprobadas").isEqualTo(180745.33);
    }

    @Test
    void sinParametrosPasaNulosAlCasoDeUso() {
        when(listarSolicitudesUseCase.listar(isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(Pagina.de(List.of(), 0, 10, 0)));

        client.get().uri(RouterRest.SOLICITUD + "?pagina=")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.contenido").isEmpty();
    }

    @Test
    void parametroNoNumericoResponde400() {
        client.get().uri(RouterRest.SOLICITUD + "?pagina=uno")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.mensaje").isEqualTo("El parámetro pagina debe ser un número entero");
        client.get().uri(RouterRest.SOLICITUD + "?tipo_prestamo=x")
                .exchange()
                .expectStatus().isBadRequest();
        verify(listarSolicitudesUseCase, never()).listar(any(), any(), any(), any(), any());
    }
}
