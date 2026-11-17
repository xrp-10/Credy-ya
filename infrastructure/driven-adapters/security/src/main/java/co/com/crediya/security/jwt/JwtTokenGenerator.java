package co.com.crediya.security.jwt;

import co.com.crediya.model.auth.gateways.TokenGenerator;
import co.com.crediya.model.usuario.Rol;
import co.com.crediya.model.usuario.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Firma un JWT con el correo como sujeto y el rol en el claim "rol". */
@Component
@RequiredArgsConstructor
public class JwtTokenGenerator implements TokenGenerator {

    public static final String CLAIM_ROL = "rol";
    public static final String CLAIM_ID_USUARIO = "id_usuario";
    private static final String EMISOR = "crediya";

    private final JwtEncoder encoder;
    private final JwtProperties properties;

    @Override
    public Mono<String> generate(Usuario usuario) {
        return Mono.fromCallable(() -> {
            Rol rol = Rol.desdeId(usuario.getIdRol())
                    .orElseThrow(() -> new IllegalStateException("El usuario tiene un rol desconocido"));
            Instant ahora = Instant.now();
            JwtClaimsSet claims = JwtClaimsSet.builder()
                    .issuer(EMISOR)
                    .subject(usuario.getEmail())
                    .issuedAt(ahora)
                    .expiresAt(ahora.plus(properties.minutosDeVigencia(), ChronoUnit.MINUTES))
                    .claim(CLAIM_ROL, rol.name())
                    .claim(CLAIM_ID_USUARIO, usuario.getIdUsuario())
                    .build();
            JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
            return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        });
    }
}
