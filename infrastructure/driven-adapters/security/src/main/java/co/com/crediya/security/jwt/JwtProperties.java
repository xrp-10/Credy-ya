package co.com.crediya.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del token. El secreto llega por la variable de entorno JWT_SECRET (mínimo 32 caracteres)
 * y nunca se guarda en el repositorio.
 */
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(String secret, Long expirationMinutes) {

    private static final long MINUTOS_POR_DEFECTO = 60;

    public long minutosDeVigencia() {
        return expirationMinutes == null || expirationMinutes <= 0 ? MINUTOS_POR_DEFECTO : expirationMinutes;
    }
}
