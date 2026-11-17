package co.com.crediya.r2dbc.solicitud;

import co.com.crediya.model.exceptions.BusinessErrorType;
import co.com.crediya.model.exceptions.BusinessException;
import co.com.crediya.model.solicitud.Solicitud;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitudRepositoryAdapterTest {

    @Mock
    private SolicitudReactiveRepository repository;

    @Mock
    private ObjectMapper mapper;

    @Mock
    private TransactionalOperator transaccion;

    private SolicitudRepositoryAdapter adapter;

    private final Solicitud solicitud = Solicitud.builder().monto(new BigDecimal("5000000")).plazo(24)
            .email("cliente@crediya.test").idEstado(1).idTipoPrestamo(1L).build();
    private final SolicitudEntity entity = SolicitudEntity.builder().monto(new BigDecimal("5000000")).plazo(24)
            .email("cliente@crediya.test").idEstado(1).idTipoPrestamo(1L).build();

    @BeforeEach
    void setUp() {
        adapter = new SolicitudRepositoryAdapter(repository, mapper, transaccion);
        when(transaccion.transactional(any(Mono.class))).thenAnswer(i -> i.getArgument(0));
        when(mapper.map(solicitud, SolicitudEntity.class)).thenReturn(entity);
    }

    @Test
    void guardaLaSolicitudDentroDeUnaTransaccion() {
        SolicitudEntity guardada = entity.toBuilder().idSolicitud(10L).build();
        when(repository.save(entity)).thenReturn(Mono.just(guardada));
        when(mapper.map(guardada, Solicitud.class)).thenReturn(solicitud.toBuilder().idSolicitud(10L).build());

        StepVerifier.create(adapter.save(solicitud))
                .expectNextMatches(s -> s.getIdSolicitud() == 10L)
                .verifyComplete();
        verify(transaccion).transactional(any(Mono.class));
    }

    @Test
    void convierteViolacionDeIntegridadEnDatosInvalidos() {
        when(repository.save(entity)).thenReturn(Mono.error(new DataIntegrityViolationException("fk")));

        StepVerifier.create(adapter.save(solicitud))
                .expectErrorMatches(e -> e instanceof BusinessException be
                        && be.getType() == BusinessErrorType.INVALID_DATA)
                .verify();
    }
}
