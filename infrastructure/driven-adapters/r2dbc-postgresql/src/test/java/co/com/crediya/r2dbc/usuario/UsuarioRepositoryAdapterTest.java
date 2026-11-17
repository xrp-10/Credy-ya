package co.com.crediya.r2dbc.usuario;

import co.com.crediya.model.exceptions.BusinessErrorType;
import co.com.crediya.model.exceptions.BusinessException;
import co.com.crediya.model.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioRepositoryAdapterTest {

    @Mock
    private UsuarioReactiveRepository repository;

    @Mock
    private ObjectMapper mapper;

    private UsuarioRepositoryAdapter adapter;

    private final Usuario usuario = Usuario.builder().nombre("Ana").email("ana@correo.com")
            .salarioBase(new BigDecimal("2500000")).build();
    private final UsuarioEntity entity = UsuarioEntity.builder().nombre("Ana").email("ana@correo.com")
            .salarioBase(new BigDecimal("2500000")).build();

    @BeforeEach
    void setUp() {
        adapter = new UsuarioRepositoryAdapter(repository, mapper);
    }

    @Test
    void guardaElUsuarioYDevuelveElId() {
        UsuarioEntity guardada = entity.toBuilder().idUsuario(1L).build();
        when(mapper.map(usuario, UsuarioEntity.class)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(Mono.just(guardada));
        when(mapper.map(guardada, Usuario.class)).thenReturn(usuario.toBuilder().idUsuario(1L).build());

        StepVerifier.create(adapter.save(usuario))
                .expectNextMatches(u -> u.getIdUsuario() == 1L)
                .verifyComplete();
    }

    @Test
    void convierteLlaveDuplicadaEnErrorDeNegocio() {
        when(mapper.map(usuario, UsuarioEntity.class)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(Mono.error(new DuplicateKeyException("duplicado")));

        StepVerifier.create(adapter.save(usuario))
                .expectErrorMatches(e -> esError(e, BusinessErrorType.ALREADY_EXISTS))
                .verify();
    }

    @Test
    void convierteOtraViolacionDeIntegridadEnDatosInvalidos() {
        when(mapper.map(usuario, UsuarioEntity.class)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(Mono.error(new DataIntegrityViolationException("check")));

        StepVerifier.create(adapter.save(usuario))
                .expectErrorMatches(e -> esError(e, BusinessErrorType.INVALID_DATA))
                .verify();
    }

    @Test
    void dejaPasarErroresTecnicos() {
        when(mapper.map(usuario, UsuarioEntity.class)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(Mono.error(new IllegalStateException("sin conexion")));

        StepVerifier.create(adapter.save(usuario))
                .expectError(IllegalStateException.class)
                .verify();
    }

    @Test
    void consultaSiElCorreoOElDocumentoYaExisten() {
        when(repository.existsByEmail("ana@correo.com")).thenReturn(Mono.just(true));
        when(repository.existsByDocumentoIdentidad("123")).thenReturn(Mono.just(false));

        StepVerifier.create(adapter.existsByEmail("ana@correo.com")).expectNext(true).verifyComplete();
        StepVerifier.create(adapter.existsByDocumentoIdentidad("123")).expectNext(false).verifyComplete();
    }

    @Test
    void buscaElUsuarioPorDocumento() {
        UsuarioEntity encontrada = entity.toBuilder().idUsuario(5L).documentoIdentidad("123").build();
        when(repository.findByDocumentoIdentidad("123")).thenReturn(Mono.just(encontrada));
        when(mapper.map(encontrada, Usuario.class)).thenReturn(usuario.toBuilder().idUsuario(5L).build());

        StepVerifier.create(adapter.findByDocumentoIdentidad("123"))
                .expectNextMatches(u -> u.getIdUsuario() == 5L)
                .verifyComplete();
    }

    private static boolean esError(Throwable e, BusinessErrorType tipo) {
        return e instanceof BusinessException be && be.getType() == tipo;
    }
}
