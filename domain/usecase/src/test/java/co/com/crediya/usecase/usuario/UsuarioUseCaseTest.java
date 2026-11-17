package co.com.crediya.usecase.usuario;

import co.com.crediya.model.auth.gateways.PasswordHasher;
import co.com.crediya.model.exceptions.BusinessErrorType;
import co.com.crediya.model.exceptions.BusinessException;
import co.com.crediya.model.usuario.Rol;
import co.com.crediya.model.usuario.Usuario;
import co.com.crediya.model.usuario.gateways.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioUseCaseTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordHasher passwordHasher;

    private UsuarioUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UsuarioUseCase(usuarioRepository, passwordHasher);
    }

    private static Usuario valido() {
        return Usuario.builder()
                .nombre("Ana María")
                .apellido("Pérez Gómez")
                .email("ana@correo.com")
                .documentoIdentidad("1020304050")
                .telefono("3001234567")
                .fechaNacimiento(LocalDate.of(1995, 4, 10))
                .direccion("Calle 10 # 5-20")
                .salarioBase(new BigDecimal("2500000"))
                .contrasena("ClaveSegura1")
                .build();
    }

    private void sinDuplicados() {
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(Mono.just(false));
        when(usuarioRepository.existsByDocumentoIdentidad(anyString())).thenReturn(Mono.just(false));
        lenient().when(passwordHasher.hash(anyString())).thenReturn(Mono.just("hash-bcrypt"));
        lenient().when(usuarioRepository.save(any())).thenAnswer(i -> Mono.just(i.getArgument(0)));
    }

    @Test
    void registraUsuarioConClaveCifradaYRolClientePorDefecto() {
        sinDuplicados();
        Usuario entrada = valido().toBuilder().idUsuario(99L).email("  Ana@Correo.COM ").build();

        StepVerifier.create(useCase.registrar(entrada))
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(guardado.capture());
        assertThat(guardado.getValue().getIdUsuario()).isNull();
        assertThat(guardado.getValue().getEmail()).isEqualTo("ana@correo.com");
        assertThat(guardado.getValue().getContrasena()).isEqualTo("hash-bcrypt");
        assertThat(guardado.getValue().getIdRol()).isEqualTo(Rol.CLIENTE.getId());
        verify(usuarioRepository).existsByEmail("ana@correo.com");
        verify(passwordHasher).hash("ClaveSegura1");
    }

    @Test
    void respetaElRolEnviado() {
        sinDuplicados();

        StepVerifier.create(useCase.registrar(valido().toBuilder().idRol(Rol.ASESOR.getId()).build()))
                .expectNextMatches(u -> u.getIdRol() == Rol.ASESOR.getId())
                .verifyComplete();
    }

    @Test
    void rechazaCorreoYaRegistrado() {
        when(usuarioRepository.existsByEmail("ana@correo.com")).thenReturn(Mono.just(true));

        StepVerifier.create(useCase.registrar(valido()))
                .expectErrorMatches(e -> esError(e, BusinessErrorType.ALREADY_EXISTS))
                .verify();
        verify(usuarioRepository, never()).existsByDocumentoIdentidad(anyString());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void rechazaDocumentoYaRegistrado() {
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(Mono.just(false));
        when(usuarioRepository.existsByDocumentoIdentidad("1020304050")).thenReturn(Mono.just(true));

        StepVerifier.create(useCase.registrar(valido()))
                .expectErrorMatches(e -> esError(e, BusinessErrorType.ALREADY_EXISTS))
                .verify();
        verify(passwordHasher, never()).hash(anyString());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void rechazaDatosInvalidos() {
        Usuario base = valido();
        List<Usuario> invalidos = List.of(
                base.toBuilder().nombre(" ").build(),
                base.toBuilder().apellido(null).build(),
                base.toBuilder().email("").build(),
                base.toBuilder().documentoIdentidad(null).build(),
                base.toBuilder().salarioBase(null).build(),
                base.toBuilder().contrasena(" ").build(),
                base.toBuilder().email("ana.correo.com").build(),
                base.toBuilder().salarioBase(new BigDecimal("-1")).build(),
                base.toBuilder().salarioBase(new BigDecimal("15000000.01")).build(),
                base.toBuilder().nombre("a".repeat(101)).build(),
                base.toBuilder().apellido("a".repeat(101)).build(),
                base.toBuilder().email("a".repeat(140) + "@correo.com").build(),
                base.toBuilder().documentoIdentidad("1".repeat(21)).build(),
                base.toBuilder().telefono("3".repeat(21)).build(),
                base.toBuilder().direccion("d".repeat(201)).build(),
                base.toBuilder().contrasena("corta12").build(),
                base.toBuilder().contrasena("x".repeat(65)).build(),
                base.toBuilder().fechaNacimiento(LocalDate.now()).build(),
                base.toBuilder().idRol(9).build()
        );

        invalidos.forEach(usuario -> StepVerifier.create(useCase.registrar(usuario))
                .expectErrorMatches(e -> esError(e, BusinessErrorType.INVALID_DATA))
                .verify());
        StepVerifier.create(useCase.registrar(null))
                .expectErrorMatches(e -> esError(e, BusinessErrorType.INVALID_DATA))
                .verify();
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void aceptaLosValoresLimite() {
        sinDuplicados();
        Usuario base = valido();
        List<Usuario> limites = List.of(
                base.toBuilder().salarioBase(BigDecimal.ZERO).build(),
                base.toBuilder().salarioBase(new BigDecimal("15000000")).build(),
                base.toBuilder().contrasena("x".repeat(8)).build(),
                base.toBuilder().contrasena("x".repeat(64)).build(),
                base.toBuilder().nombre("a".repeat(100)).apellido("b".repeat(100)).build(),
                base.toBuilder().documentoIdentidad("1".repeat(20)).telefono("3".repeat(20)).build(),
                base.toBuilder().direccion("d".repeat(200)).fechaNacimiento(null).build(),
                base.toBuilder().telefono(null).direccion(null).build()
        );

        limites.forEach(usuario -> StepVerifier.create(useCase.registrar(usuario))
                .expectNextCount(1)
                .verifyComplete());
    }

    private static boolean esError(Throwable e, BusinessErrorType tipo) {
        return e instanceof BusinessException be && be.getType() == tipo;
    }
}
