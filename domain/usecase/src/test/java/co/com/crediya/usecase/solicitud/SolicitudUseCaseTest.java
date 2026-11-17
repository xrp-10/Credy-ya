package co.com.crediya.usecase.solicitud;

import co.com.crediya.model.exceptions.BusinessErrorType;
import co.com.crediya.model.exceptions.BusinessException;
import co.com.crediya.model.solicitud.EstadoSolicitud;
import co.com.crediya.model.solicitud.Solicitud;
import co.com.crediya.model.solicitud.gateways.SolicitudRepository;
import co.com.crediya.model.tipoprestamo.TipoPrestamo;
import co.com.crediya.model.tipoprestamo.gateways.TipoPrestamoRepository;
import co.com.crediya.model.usuario.Usuario;
import co.com.crediya.model.usuario.gateways.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitudUseCaseTest {

    @Mock
    private SolicitudRepository solicitudRepository;

    @Mock
    private TipoPrestamoRepository tipoPrestamoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    private SolicitudUseCase useCase;

    private static final String DOCUMENTO = "900000001";
    private final Usuario cliente = Usuario.builder().idUsuario(1L).email("cliente@crediya.test")
            .documentoIdentidad(DOCUMENTO).build();
    private final TipoPrestamo libreInversion = TipoPrestamo.builder().idTipoPrestamo(1L).nombre("Libre inversión")
            .montoMinimo(new BigDecimal("1000000")).montoMaximo(new BigDecimal("50000000"))
            .tasaInteres(new BigDecimal("1.80")).validacionAutomatica(true).build();

    @BeforeEach
    void setUp() {
        useCase = new SolicitudUseCase(solicitudRepository, tipoPrestamoRepository, usuarioRepository);
    }

    private static Solicitud solicitud(String monto) {
        return Solicitud.builder().monto(new BigDecimal(monto)).plazo(24).idTipoPrestamo(1L).build();
    }

    private void datosExistentes() {
        lenient().when(usuarioRepository.findByDocumentoIdentidad(DOCUMENTO)).thenReturn(Mono.just(cliente));
        lenient().when(tipoPrestamoRepository.findById(1L)).thenReturn(Mono.just(libreInversion));
        lenient().when(solicitudRepository.save(any()))
                .thenAnswer(i -> Mono.just(((Solicitud) i.getArgument(0)).toBuilder().idSolicitud(10L).build()));
    }

    @Test
    void registraLaSolicitudPendienteDeRevisionConElCorreoDelCliente() {
        datosExistentes();
        Solicitud entrada = solicitud("5000000").toBuilder().idSolicitud(77L).idEstado(4).email("otro@x.com").build();

        StepVerifier.create(useCase.registrar(entrada, "  " + DOCUMENTO + " "))
                .expectNextMatches(s -> s.getIdSolicitud() == 10L
                        && "cliente@crediya.test".equals(s.getEmail())
                        && s.getIdEstado() == EstadoSolicitud.PENDIENTE_REVISION.getId()
                        && s.getPlazo() == 24)
                .verifyComplete();
        verify(solicitudRepository).save(any());
    }

    @Test
    void aceptaMontosEnLosLimitesDelTipo() {
        datosExistentes();

        StepVerifier.create(useCase.registrar(solicitud("1000000"), DOCUMENTO)).expectNextCount(1).verifyComplete();
        StepVerifier.create(useCase.registrar(solicitud("50000000"), DOCUMENTO)).expectNextCount(1).verifyComplete();
    }

    @Test
    void rechazaMontoFueraDelRangoDelTipo() {
        datosExistentes();

        List.of("999999.99", "50000000.01").forEach(monto ->
                StepVerifier.create(useCase.registrar(solicitud(monto), DOCUMENTO))
                        .expectErrorMatches(e -> esError(e, BusinessErrorType.INVALID_DATA)
                                && e.getMessage().contains("Libre inversión"))
                        .verify());
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void rechazaClienteInexistente() {
        when(usuarioRepository.findByDocumentoIdentidad(DOCUMENTO)).thenReturn(Mono.empty());
        when(tipoPrestamoRepository.findById(1L)).thenReturn(Mono.just(libreInversion));

        StepVerifier.create(useCase.registrar(solicitud("5000000"), DOCUMENTO))
                .expectErrorMatches(e -> esError(e, BusinessErrorType.NOT_FOUND))
                .verify();
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void rechazaTipoDePrestamoInexistente() {
        when(usuarioRepository.findByDocumentoIdentidad(DOCUMENTO)).thenReturn(Mono.just(cliente));
        when(tipoPrestamoRepository.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.registrar(solicitud("5000000").toBuilder().idTipoPrestamo(99L).build(), DOCUMENTO))
                .expectErrorMatches(e -> esError(e, BusinessErrorType.INVALID_DATA)
                        && e.getMessage().contains("no existe"))
                .verify();
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void rechazaDatosIncompletosOInvalidos() {
        Solicitud base = solicitud("5000000");
        List<Solicitud> invalidas = List.of(
                base.toBuilder().monto(null).build(),
                base.toBuilder().monto(BigDecimal.ZERO).build(),
                base.toBuilder().monto(new BigDecimal("-5")).build(),
                base.toBuilder().plazo(null).build(),
                base.toBuilder().plazo(0).build(),
                base.toBuilder().idTipoPrestamo(null).build());

        invalidas.forEach(s -> StepVerifier.create(useCase.registrar(s, DOCUMENTO))
                .expectErrorMatches(e -> esError(e, BusinessErrorType.INVALID_DATA))
                .verify());
        StepVerifier.create(useCase.registrar(null, DOCUMENTO))
                .expectErrorMatches(e -> esError(e, BusinessErrorType.INVALID_DATA)).verify();
        StepVerifier.create(useCase.registrar(base, " "))
                .expectErrorMatches(e -> esError(e, BusinessErrorType.INVALID_DATA)).verify();
        StepVerifier.create(useCase.registrar(base, null))
                .expectErrorMatches(e -> esError(e, BusinessErrorType.INVALID_DATA)).verify();
        verify(usuarioRepository, never()).findByDocumentoIdentidad(any());
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void aceptaPlazoDeUnMes() {
        datosExistentes();

        StepVerifier.create(useCase.registrar(solicitud("5000000").toBuilder().plazo(1).build(), DOCUMENTO))
                .expectNextCount(1)
                .verifyComplete();
    }

    private static boolean esError(Throwable e, BusinessErrorType tipo) {
        return e instanceof BusinessException be && be.getType() == tipo;
    }
}
