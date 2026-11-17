package co.com.crediya.usecase.usuario;

import co.com.crediya.model.auth.gateways.PasswordHasher;
import co.com.crediya.model.exceptions.BusinessErrorType;
import co.com.crediya.model.exceptions.BusinessException;
import co.com.crediya.model.usuario.Rol;
import co.com.crediya.model.usuario.Usuario;
import co.com.crediya.model.usuario.gateways.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class UsuarioUseCase {

    private static final BigDecimal SALARIO_MINIMO = BigDecimal.ZERO;
    private static final BigDecimal SALARIO_MAXIMO = new BigDecimal("15000000");
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)*\\.[A-Za-z]{2,}$");
    private static final int CLAVE_MINIMA = 8;
    private static final int CLAVE_MAXIMA = 64;
    private static final int MAX_NOMBRE = 100;
    private static final int MAX_EMAIL = 150;
    private static final int MAX_DOCUMENTO = 20;
    private static final int MAX_TELEFONO = 20;
    private static final int MAX_DIRECCION = 200;

    private static final List<Regla> REGLAS = List.of(
            new Regla(u -> vacio(u.getNombre()), "El campo nombres es obligatorio"),
            new Regla(u -> vacio(u.getApellido()), "El campo apellidos es obligatorio"),
            new Regla(u -> vacio(u.getEmail()), "El campo correo_electronico es obligatorio"),
            new Regla(u -> vacio(u.getDocumentoIdentidad()), "El campo documento_identidad es obligatorio"),
            new Regla(u -> u.getSalarioBase() == null, "El campo salario_base es obligatorio"),
            new Regla(u -> vacio(u.getContrasena()), "El campo clave es obligatorio"),
            new Regla(u -> !EMAIL.matcher(u.getEmail().trim()).matches(),
                    "El correo_electronico no tiene un formato válido"),
            new Regla(u -> u.getSalarioBase().compareTo(SALARIO_MINIMO) < 0
                    || u.getSalarioBase().compareTo(SALARIO_MAXIMO) > 0,
                    "El salario_base debe estar entre 0 y 15000000"),
            new Regla(u -> muyLargo(u.getNombre(), MAX_NOMBRE) || muyLargo(u.getApellido(), MAX_NOMBRE),
                    "Los nombres y apellidos admiten máximo 100 caracteres"),
            new Regla(u -> muyLargo(u.getEmail(), MAX_EMAIL), "El correo_electronico admite máximo 150 caracteres"),
            new Regla(u -> muyLargo(u.getDocumentoIdentidad(), MAX_DOCUMENTO),
                    "El documento_identidad admite máximo 20 caracteres"),
            new Regla(u -> muyLargo(u.getTelefono(), MAX_TELEFONO), "El telefono admite máximo 20 caracteres"),
            new Regla(u -> muyLargo(u.getDireccion(), MAX_DIRECCION), "La direccion admite máximo 200 caracteres"),
            new Regla(u -> u.getContrasena().length() < CLAVE_MINIMA || u.getContrasena().length() > CLAVE_MAXIMA,
                    "La clave debe tener entre 8 y 64 caracteres"),
            new Regla(u -> u.getFechaNacimiento() != null && !u.getFechaNacimiento().isBefore(LocalDate.now()),
                    "La fecha_nacimiento debe ser anterior a hoy"),
            new Regla(u -> u.getIdRol() != null && Rol.desdeId(u.getIdRol()).isEmpty(), "El id_rol no existe")
    );

    private final UsuarioRepository usuarioRepository;
    private final PasswordHasher passwordHasher;

    public Mono<Usuario> registrar(Usuario usuario) {
        return validar(usuario)
                .then(Mono.fromSupplier(() -> normalizar(usuario)))
                .flatMap(nuevo -> exigirQueNoExista(usuarioRepository.existsByEmail(nuevo.getEmail()),
                        "El correo electrónico ya está registrado")
                        .then(Mono.defer(() -> exigirQueNoExista(
                                usuarioRepository.existsByDocumentoIdentidad(nuevo.getDocumentoIdentidad()),
                                "El documento de identidad ya está registrado")))
                        .then(Mono.defer(() -> passwordHasher.hash(nuevo.getContrasena())))
                        .map(hash -> nuevo.toBuilder().contrasena(hash).build()))
                .flatMap(usuarioRepository::save);
    }

    private static Mono<Void> validar(Usuario usuario) {
        if (usuario == null) {
            return invalido("Los datos del usuario son obligatorios");
        }
        return REGLAS.stream()
                .filter(regla -> regla.incumple().test(usuario))
                .findFirst()
                .map(regla -> invalido(regla.mensaje()))
                .orElse(Mono.empty());
    }

    private static Usuario normalizar(Usuario usuario) {
        return usuario.toBuilder()
                .idUsuario(null)
                .nombre(usuario.getNombre().trim())
                .apellido(usuario.getApellido().trim())
                .email(usuario.getEmail().trim().toLowerCase(Locale.ROOT))
                .documentoIdentidad(usuario.getDocumentoIdentidad().trim())
                .idRol(usuario.getIdRol() == null ? Rol.CLIENTE.getId() : usuario.getIdRol())
                .build();
    }

    private static Mono<Void> exigirQueNoExista(Mono<Boolean> existe, String mensaje) {
        return existe.flatMap(siExiste -> Boolean.TRUE.equals(siExiste)
                ? Mono.error(new BusinessException(BusinessErrorType.ALREADY_EXISTS, mensaje))
                : Mono.empty());
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private static boolean muyLargo(String valor, int maximo) {
        return valor != null && valor.length() > maximo;
    }

    private static Mono<Void> invalido(String mensaje) {
        return Mono.error(new BusinessException(BusinessErrorType.INVALID_DATA, mensaje));
    }

    private record Regla(Predicate<Usuario> incumple, String mensaje) {
    }
}
