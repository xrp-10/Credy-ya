package co.com.crediya.r2dbc.tipoprestamo;

import co.com.crediya.model.tipoprestamo.TipoPrestamo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivecommons.utils.ObjectMapper;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TipoPrestamoRepositoryAdapterTest {

    @Mock
    private TipoPrestamoReactiveRepository repository;

    @Mock
    private ObjectMapper mapper;

    @Test
    void buscaElTipoDePrestamoPorId() {
        TipoPrestamoRepositoryAdapter adapter = new TipoPrestamoRepositoryAdapter(repository, mapper);
        TipoPrestamoEntity entity = TipoPrestamoEntity.builder().idTipoPrestamo(1L).nombre("Libre inversión").build();
        when(repository.findById(1L)).thenReturn(Mono.just(entity));
        when(mapper.map(entity, TipoPrestamo.class))
                .thenReturn(TipoPrestamo.builder().idTipoPrestamo(1L).nombre("Libre inversión").build());
        when(repository.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(adapter.findById(1L))
                .expectNextMatches(t -> "Libre inversión".equals(t.getNombre()))
                .verifyComplete();
        StepVerifier.create(adapter.findById(99L)).verifyComplete();
    }
}
