package co.com.crediya.api.config;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class SecurityHeadersConfig implements WebFilter {

    static final String CSP = "default-src 'self'; frame-ancestors 'self'; form-action 'self'";
    /** Swagger UI usa íconos en data: URIs; solo sus páginas permiten imágenes data:. */
    static final String CSP_DOCUMENTACION = "default-src 'self'; img-src 'self' data:; frame-ancestors 'self'; form-action 'self'";

    private static boolean esDocumentacion(ServerWebExchange exchange) {
        String ruta = exchange.getRequest().getPath().value();
        return ruta.startsWith("/webjars/swagger-ui") || ruta.startsWith("/swagger-ui");
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        HttpHeaders headers = exchange.getResponse().getHeaders();
        headers.set("Content-Security-Policy", esDocumentacion(exchange) ? CSP_DOCUMENTACION : CSP);
        headers.set("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
        headers.set("X-Content-Type-Options", "nosniff");
        headers.remove("Server");
        headers.set("Cache-Control", "no-store");
        headers.set("Pragma", "no-cache");
        headers.set("Referrer-Policy", "strict-origin-when-cross-origin");
        return chain.filter(exchange);
    }
}
