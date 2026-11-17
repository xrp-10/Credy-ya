package co.com.crediya.usecase.login;

import co.com.crediya.model.auth.gateways.PasswordHasher;
import co.com.crediya.model.auth.gateways.TokenGenerator;
import co.com.crediya.model.exceptions.BusinessErrorType;
import co.com.crediya.model.exceptions.BusinessException;
import co.com.crediya.model.usuario.gateways.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.Locale;

/**
 * Inicio de sesión con correo y clave. Los intentos son ilimitados (lo pide la HU3) y el mensaje
 * de error es el mismo si el correo no existe o si la clave es incorrecta, para no revelar cuentas.
 */
@RequiredArgsConstructor
public class LoginUseCase {

    private static final String CREDENCIALES_INVALIDAS = "Correo o clave incorrectos";

    private final UsuarioRepository usuarioRepository;
    private final PasswordHasher passwordHasher;
    private final TokenGenerator tokenGenerator;

    public Mono<String> login(String email, String clave) {
        if (vacio(email) || vacio(clave)) {
            return Mono.error(new BusinessException(BusinessErrorType.INVALID_DATA,
                    "El correo_electronico y la clave son obligatorios"));
        }
        return usuarioRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .filterWhen(usuario -> passwordHasher.matches(clave, usuario.getContrasena()))
                .switchIfEmpty(Mono.error(new BusinessException(BusinessErrorType.UNAUTHORIZED, CREDENCIALES_INVALIDAS)))
                .flatMap(tokenGenerator::generate);
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
