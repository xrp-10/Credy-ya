package co.com.crediya.usecase.login;

import co.com.crediya.model.auth.gateways.PasswordHasher;
import co.com.crediya.model.auth.gateways.TokenGenerator;
import co.com.crediya.model.exceptions.BusinessErrorType;
import co.com.crediya.model.exceptions.BusinessException;
import co.com.crediya.model.usuario.Usuario;
import co.com.crediya.model.usuario.gateways.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private TokenGenerator tokenGenerator;

    private LoginUseCase useCase;

    private final Usuario usuario = Usuario.builder().idUsuario(1L).email("ana@correo.com")
            .contrasena("hash-bcrypt").idRol(3).build();

    @BeforeEach
    void setUp() {
        useCase = new LoginUseCase(usuarioRepository, passwordHasher, tokenGenerator);
    }

    @Test
    void devuelveElTokenConCorreoYClaveCorrectos() {
        when(usuarioRepository.findByEmail("ana@correo.com")).thenReturn(Mono.just(usuario));
        when(passwordHasher.matches("ClaveSegura1", "hash-bcrypt")).thenReturn(Mono.just(true));
        when(tokenGenerator.generate(usuario)).thenReturn(Mono.just("jwt"));

        StepVerifier.create(useCase.login("  Ana@Correo.com ", "ClaveSegura1"))
                .expectNext("jwt")
                .verifyComplete();
    }

    @Test
    void rechazaClaveIncorrectaSinGenerarToken() {
        when(usuarioRepository.findByEmail("ana@correo.com")).thenReturn(Mono.just(usuario));
        when(passwordHasher.matches("Equivocada1", "hash-bcrypt")).thenReturn(Mono.just(false));

        StepVerifier.create(useCase.login("ana@correo.com", "Equivocada1"))
                .expectErrorMatches(e -> esError(e, BusinessErrorType.UNAUTHORIZED)
                        && "Correo o clave incorrectos".equals(e.getMessage()))
                .verify();
        verify(tokenGenerator, never()).generate(any());
    }

    @Test
    void rechazaCorreoInexistenteConElMismoMensaje() {
        when(usuarioRepository.findByEmail("nadie@correo.com")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.login("nadie@correo.com", "ClaveSegura1"))
                .expectErrorMatches(e -> esError(e, BusinessErrorType.UNAUTHORIZED)
                        && "Correo o clave incorrectos".equals(e.getMessage()))
                .verify();
        verify(passwordHasher, never()).matches(anyString(), anyString());
    }

    @Test
    void permiteIntentosIlimitados() {
        when(usuarioRepository.findByEmail("ana@correo.com")).thenReturn(Mono.just(usuario));
        when(passwordHasher.matches("Equivocada1", "hash-bcrypt")).thenReturn(Mono.just(false));
        when(passwordHasher.matches("ClaveSegura1", "hash-bcrypt")).thenReturn(Mono.just(true));
        when(tokenGenerator.generate(usuario)).thenReturn(Mono.just("jwt"));

        for (int intento = 0; intento < 10; intento++) {
            StepVerifier.create(useCase.login("ana@correo.com", "Equivocada1"))
                    .expectError(BusinessException.class)
                    .verify();
        }
        StepVerifier.create(useCase.login("ana@correo.com", "ClaveSegura1"))
                .expectNext("jwt")
                .verifyComplete();
        verify(usuarioRepository, times(11)).findByEmail("ana@correo.com");
    }

    @Test
    void exigeCorreoYClave() {
        StepVerifier.create(useCase.login(" ", "ClaveSegura1"))
                .expectErrorMatches(e -> esError(e, BusinessErrorType.INVALID_DATA)).verify();
        StepVerifier.create(useCase.login("ana@correo.com", null))
                .expectErrorMatches(e -> esError(e, BusinessErrorType.INVALID_DATA)).verify();
        StepVerifier.create(useCase.login(null, "ClaveSegura1"))
                .expectErrorMatches(e -> esError(e, BusinessErrorType.INVALID_DATA)).verify();
        StepVerifier.create(useCase.login("ana@correo.com", ""))
                .expectErrorMatches(e -> esError(e, BusinessErrorType.INVALID_DATA)).verify();
        verify(usuarioRepository, never()).findByEmail(anyString());
    }

    private static boolean esError(Throwable e, BusinessErrorType tipo) {
        return e instanceof BusinessException be && be.getType() == tipo;
    }
}
