package co.com.crediya.api.config;

import co.com.crediya.api.RouterRest;
import co.com.crediya.api.error.ErrorResponseWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Quién puede llamar qué (HU3). Sin sesión en servidor: cada petición trae su JWT en
 * "Authorization: Bearer ...", y el rol sale del claim "rol" del token.
 */
@Slf4j
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    static final String ADMIN = "ADMIN";
    static final String ASESOR = "ASESOR";
    static final String CLIENTE = "CLIENTE";

    private final ErrorResponseWriter errorWriter;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, ReactiveJwtDecoder jwtDecoder) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .authorizeExchange(reglas -> reglas
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        .pathMatchers(HttpMethod.POST, RouterRest.LOGIN).permitAll()
                        .pathMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .pathMatchers(HttpMethod.POST, RouterRest.USUARIOS).hasAnyRole(ADMIN, ASESOR)
                        .pathMatchers(HttpMethod.POST, RouterRest.SOLICITUD).hasRole(CLIENTE)
                        .pathMatchers(HttpMethod.GET, RouterRest.SOLICITUD).hasRole(ASESOR)
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtDecoder(jwtDecoder).jwtAuthenticationConverter(rolesDelToken()))
                        .authenticationEntryPoint(this::sinSesion)
                        .accessDeniedHandler(this::sinPermiso))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(this::sinSesion)
                        .accessDeniedHandler(this::sinPermiso))
                .build();
    }

    static Converter<Jwt, Mono<AbstractAuthenticationToken>> rolesDelToken() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("rol");
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter(roles);
        return new ReactiveJwtAuthenticationConverterAdapter(convertidor);
    }

    private Mono<Void> sinSesion(ServerWebExchange exchange, AuthenticationException error) {
        log.warn("Acceso sin sesión válida a {}: {}", exchange.getRequest().getPath(), error.getMessage());
        return errorWriter.write(exchange, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                "Debes iniciar sesión y enviar un token válido");
    }

    private Mono<Void> sinPermiso(ServerWebExchange exchange, AccessDeniedException error) {
        log.warn("Acceso denegado por rol a {}", exchange.getRequest().getPath());
        return errorWriter.write(exchange, HttpStatus.FORBIDDEN, "FORBIDDEN",
                "Tu rol no tiene permiso para esta acción");
    }
}
