package co.com.crediya.api.config;

import co.com.crediya.api.RouterRest;
import co.com.crediya.api.auth.AuthHandler;
import co.com.crediya.api.error.ErrorResponseWriter;
import co.com.crediya.api.error.GlobalErrorHandler;
import co.com.crediya.api.revision.RevisionHandler;
import co.com.crediya.api.solicitud.SolicitudHandler;
import co.com.crediya.api.usuario.UsuarioHandler;
import co.com.crediya.model.pagina.Pagina;
import co.com.crediya.model.solicitud.Solicitud;
import co.com.crediya.model.usuario.Usuario;
import co.com.crediya.usecase.listarsolicitudes.ListarSolicitudesUseCase;
import co.com.crediya.usecase.login.LoginUseCase;
import co.com.crediya.usecase.solicitud.SolicitudUseCase;
import co.com.crediya.usecase.usuario.UsuarioUseCase;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.config.EnableWebFlux;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Levanta la configuración real de seguridad con las rutas y casos de uso simulados. */
class SecurityConfigTest {

    private static final SecretKey LLAVE = new SecretKeySpec(
            "llave-de-pruebas-de-seguridad-con-32-bytes-o-mas".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    private static final String USUARIO = "{\"nombres\": \"Ana\"}";
    private static final String SOLICITUD = "{\"documento_identidad\": \"1\", \"monto\": 1, \"plazo\": 1, \"id_tipo_prestamo\": 1}";

    private static AnnotationConfigApplicationContext contexto;
    private static WebTestClient client;

    @BeforeAll
    static void iniciar() {
        contexto = new AnnotationConfigApplicationContext(Contexto.class);
        client = WebTestClient.bindToApplicationContext(contexto).build();
    }

    @AfterAll
    static void cerrar() {
        contexto.close();
    }

    @Test
    void elLoginEsPublico() {
        client.post().uri(RouterRest.LOGIN).contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"correo_electronico\": \"a@crediya.test\", \"clave\": \"ClaveSegura1\"}")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void sinTokenOConTokenFalsoResponde401EnJson() {
        client.post().uri(RouterRest.USUARIOS).contentType(MediaType.APPLICATION_JSON).bodyValue(USUARIO)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.error").isEqualTo("UNAUTHORIZED");
        client.post().uri(RouterRest.SOLICITUD).header(HttpHeaders.AUTHORIZATION, "Bearer token-falso")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(SOLICITUD)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.error").isEqualTo("UNAUTHORIZED");
    }

    @Test
    void soloAdminYAsesorRegistranUsuarios() {
        List.of("ADMIN", "ASESOR").forEach(rol -> post(RouterRest.USUARIOS, USUARIO, rol)
                .expectStatus().isCreated());
        post(RouterRest.USUARIOS, USUARIO, "CLIENTE")
                .expectStatus().isForbidden()
                .expectBody().jsonPath("$.error").isEqualTo("FORBIDDEN");
    }

    @Test
    void soloElClienteCreaSolicitudes() {
        post(RouterRest.SOLICITUD, SOLICITUD, "CLIENTE").expectStatus().isCreated();
        List.of("ADMIN", "ASESOR").forEach(rol -> post(RouterRest.SOLICITUD, SOLICITUD, rol)
                .expectStatus().isForbidden());
    }

    @Test
    void soloElAsesorVeElListadoDeRevision() {
        client.get().uri(RouterRest.SOLICITUD).header(HttpHeaders.AUTHORIZATION, "Bearer " + token("ASESOR"))
                .exchange()
                .expectStatus().isOk();
        List.of("ADMIN", "CLIENTE").forEach(rol -> client.get().uri(RouterRest.SOLICITUD)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(rol))
                .exchange()
                .expectStatus().isForbidden());
        client.get().uri(RouterRest.SOLICITUD).exchange().expectStatus().isUnauthorized();
    }

    @Test
    void laSaludDeLaAppEsPublicaYLoDemasPideSesion() {
        client.get().uri("/actuator/health").exchange().expectStatus().isNotFound();
        client.get().uri("/api/v1/otra-cosa").exchange().expectStatus().isUnauthorized();
    }

    private static WebTestClient.ResponseSpec post(String ruta, String cuerpo, String rol) {
        return client.post().uri(ruta)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(rol))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(cuerpo)
                .exchange();
    }

    private static String token(String rol) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(rol.toLowerCase(Locale.ROOT) + "@crediya.test")
                .issuedAt(ahora)
                .expiresAt(ahora.plusSeconds(600))
                .claim("rol", rol)
                .build();
        return new NimbusJwtEncoder(new ImmutableSecret<>(LLAVE))
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    @Configuration
    @EnableWebFlux
    @Import({SecurityConfig.class, RouterRest.class, ErrorResponseWriter.class, GlobalErrorHandler.class})
    static class Contexto {

        @Bean
        ReactiveJwtDecoder reactiveJwtDecoder() {
            return NimbusReactiveJwtDecoder.withSecretKey(LLAVE).macAlgorithm(MacAlgorithm.HS256).build();
        }

        @Bean
        UsuarioHandler usuarioHandler() {
            UsuarioUseCase useCase = mock(UsuarioUseCase.class);
            when(useCase.registrar(any())).thenReturn(Mono.just(Usuario.builder().idUsuario(1L).idRol(3).build()));
            return new UsuarioHandler(useCase);
        }

        @Bean
        SolicitudHandler solicitudHandler() {
            SolicitudUseCase useCase = mock(SolicitudUseCase.class);
            when(useCase.registrar(any(), any(), any()))
                    .thenReturn(Mono.just(Solicitud.builder().idSolicitud(1L).idEstado(1).build()));
            return new SolicitudHandler(useCase);
        }

        @Bean
        RevisionHandler revisionHandler() {
            ListarSolicitudesUseCase useCase = mock(ListarSolicitudesUseCase.class);
            when(useCase.listar(any(), any(), any(), any(), any())).thenReturn(Mono.just(Pagina.de(List.of(), 0, 10, 0)));
            return new RevisionHandler(useCase);
        }

        @Bean
        AuthHandler authHandler() {
            LoginUseCase useCase = mock(LoginUseCase.class);
            when(useCase.login(any(), any())).thenReturn(Mono.just("jwt"));
            return new AuthHandler(useCase);
        }
    }
}
