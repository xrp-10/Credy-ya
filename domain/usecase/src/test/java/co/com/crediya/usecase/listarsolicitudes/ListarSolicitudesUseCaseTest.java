package co.com.crediya.usecase.listarsolicitudes;

import co.com.crediya.model.exceptions.BusinessErrorType;
import co.com.crediya.model.exceptions.BusinessException;
import co.com.crediya.model.solicitudrevision.FiltroRevision;
import co.com.crediya.model.solicitudrevision.SolicitudRevision;
import co.com.crediya.model.solicitudrevision.gateways.SolicitudRevisionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListarSolicitudesUseCaseTest {

    @Mock
    private SolicitudRevisionRepository repository;

    private ListarSolicitudesUseCase useCase;

    private final SolicitudRevision fila = SolicitudRevision.builder().idSolicitud(1L).monto(new BigDecimal("5000000"))
            .plazo(24).email("cliente@crediya.test").estadoSolicitud("Pendiente de revisión").build();

    @BeforeEach
    void setUp() {
        useCase = new ListarSolicitudesUseCase(repository);
    }

    private void conResultados(long total) {
        when(repository.findForReview(any())).thenReturn(Flux.just(fila));
        when(repository.countForReview(any())).thenReturn(Mono.just(total));
    }

    @Test
    void porDefectoListaLosTresEstadosEnRevisionPrimeraPaginaDeDiez() {
        conResultados(23);

        StepVerifier.create(useCase.listar(null, null, null, null, null))
                .expectNextMatches(p -> p.contenido().size() == 1 && p.pagina() == 0 && p.tamano() == 10
                        && p.totalElementos() == 23 && p.totalPaginas() == 3)
                .verifyComplete();

        ArgumentCaptor<FiltroRevision> filtro = ArgumentCaptor.forClass(FiltroRevision.class);
        verify(repository).findForReview(filtro.capture());
        assertThat(filtro.getValue().getEstados()).containsExactly(1, 2, 3);
        assertThat(filtro.getValue().getEmail()).isNull();
        assertThat(filtro.getValue().getIdTipoPrestamo()).isNull();
        assertThat(filtro.getValue().getPagina()).isZero();
        assertThat(filtro.getValue().getTamano()).isEqualTo(10);
    }

    @Test
    void aplicaLosFiltrosYLaPaginaPedida() {
        conResultados(51);

        StepVerifier.create(useCase.listar(3, "  Cliente@CrediYa.test ", 2L, 1, 50))
                .expectNextMatches(p -> p.pagina() == 1 && p.tamano() == 50 && p.totalPaginas() == 2)
                .verifyComplete();

        ArgumentCaptor<FiltroRevision> filtro = ArgumentCaptor.forClass(FiltroRevision.class);
        verify(repository).countForReview(filtro.capture());
        assertThat(filtro.getValue().getEstados()).containsExactly(3);
        assertThat(filtro.getValue().getEmail()).isEqualTo("cliente@crediya.test");
        assertThat(filtro.getValue().getIdTipoPrestamo()).isEqualTo(2L);
        assertThat(filtro.getValue().getPagina()).isEqualTo(1);
    }

    @Test
    void sinResultadosDevuelvePaginaVacia() {
        when(repository.findForReview(any())).thenReturn(Flux.empty());
        when(repository.countForReview(any())).thenReturn(Mono.just(0L));

        StepVerifier.create(useCase.listar(null, " ", null, 0, 1))
                .expectNextMatches(p -> p.contenido().isEmpty() && p.totalPaginas() == 0 && p.tamano() == 1)
                .verifyComplete();
    }

    @Test
    void rechazaParametrosInvalidos() {
        List.of(
                useCase.listar(null, null, null, -1, 10),
                useCase.listar(null, null, null, 0, 0),
                useCase.listar(null, null, null, 0, 51),
                useCase.listar(4, null, null, 0, 10),
                useCase.listar(0, null, null, 0, 10)
        ).forEach(resultado -> StepVerifier.create(resultado)
                .expectErrorMatches(e -> e instanceof BusinessException be
                        && be.getType() == BusinessErrorType.INVALID_DATA)
                .verify());
        verify(repository, never()).findForReview(any());
    }
}
