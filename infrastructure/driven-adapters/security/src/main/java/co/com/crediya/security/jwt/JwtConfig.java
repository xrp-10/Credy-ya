package co.com.crediya.security.jwt;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/** Llave HS256 compartida por quien firma los tokens (login) y quien los valida (Spring Security). */
@Slf4j
@Configuration
public class JwtConfig {

    static final int BYTES_MINIMOS = 32;
    private static final String ALGORITMO = "HmacSHA256";

    @Bean
    public SecretKey jwtSecretKey(JwtProperties properties) {
        return crearLlave(properties.secret());
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder(SecretKey jwtSecretKey) {
        return NimbusReactiveJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    static SecretKey crearLlave(String secreto) {
        if (secreto == null || secreto.isBlank()) {
            log.warn("JWT_SECRET no está configurado: se usa una llave temporal y los tokens dejan de servir al reiniciar");
            byte[] aleatorio = new byte[BYTES_MINIMOS];
            new SecureRandom().nextBytes(aleatorio);
            return new SecretKeySpec(aleatorio, ALGORITMO);
        }
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < BYTES_MINIMOS) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos " + BYTES_MINIMOS + " caracteres");
        }
        return new SecretKeySpec(bytes, ALGORITMO);
    }
}
