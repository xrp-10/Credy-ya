package co.com.crediya.api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/** Log de traza de cada petición: método, ruta, estado HTTP y duración. */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        long inicio = System.currentTimeMillis();
        String metodo = exchange.getRequest().getMethod().name();
        String ruta = exchange.getRequest().getPath().value();
        exchange.getResponse().beforeCommit(() -> {
            log.info("{} {} -> {} ({} ms)", metodo, ruta, exchange.getResponse().getStatusCode(),
                    System.currentTimeMillis() - inicio);
            return Mono.empty();
        });
        return chain.filter(exchange);
    }
}
