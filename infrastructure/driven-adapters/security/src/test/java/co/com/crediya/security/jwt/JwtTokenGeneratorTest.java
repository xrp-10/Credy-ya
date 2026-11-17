package co.com.crediya.security.jwt;

import co.com.crediya.model.usuario.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenGeneratorTest {

    private static final String SECRETO = "secreto-de-pruebas-con-mas-de-32-caracteres";

    private final JwtConfig config = new JwtConfig();
    private final SecretKey llave = config.jwtSecretKey(new JwtProperties(SECRETO, 30L));
    private final JwtTokenGenerator generador =
            new JwtTokenGenerator(config.jwtEncoder(llave), new JwtProperties(SECRETO, 30L));
    private final ReactiveJwtDecoder decoder = config.reactiveJwtDecoder(llave);

    private final Usuario asesor = Usuario.builder().idUsuario(4L).email("asesor@crediya.test").idRol(2).build();

    @Test
    void generaUnTokenFirmadoConCorreoRolYVigencia() {
        String token = generador.generate(asesor).block();

        Jwt jwt = decoder.decode(token).block();
        assertThat(jwt.getSubject()).isEqualTo("asesor@crediya.test");
        assertThat(jwt.getClaimAsString(JwtTokenGenerator.CLAIM_ROL)).isEqualTo("ASESOR");
        assertThat(jwt.getClaimAsString(JwtTokenGenerator.CLAIM_ID_USUARIO)).isEqualTo("4");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void unTokenFirmadoConOtraLlaveNoEsValido() {
        SecretKey otra = config.jwtSecretKey(new JwtProperties("otro-secreto-distinto-con-mas-de-32-caracteres", null));
        String ajeno = new JwtTokenGenerator(config.jwtEncoder(otra), new JwtProperties(null, null))
                .generate(asesor).block();

        StepVerifier.create(decoder.decode(ajeno)).expectError().verify();
    }

    @Test
    void rechazaUsuarioConRolDesconocido() {
        StepVerifier.create(generador.generate(asesor.toBuilder().idRol(9).build()))
                .expectError(IllegalStateException.class)
                .verify();
    }

    @Test
    void validaElSecretoYUsaVigenciaPorDefecto() {
        assertThatThrownBy(() -> JwtConfig.crearLlave("corto")).isInstanceOf(IllegalStateException.class);
        assertThat(JwtConfig.crearLlave(" ").getEncoded()).hasSize(JwtConfig.BYTES_MINIMOS);
        assertThat(JwtConfig.crearLlave(null).getEncoded()).hasSize(JwtConfig.BYTES_MINIMOS);
        assertThat(new JwtProperties(null, null).minutosDeVigencia()).isEqualTo(60);
        assertThat(new JwtProperties(null, 0L).minutosDeVigencia()).isEqualTo(60);
        assertThat(new JwtProperties(null, 15L).minutosDeVigencia()).isEqualTo(15);
    }
}
