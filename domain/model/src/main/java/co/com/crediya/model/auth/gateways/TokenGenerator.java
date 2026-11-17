package co.com.crediya.model.auth.gateways;

import co.com.crediya.model.usuario.Usuario;
import reactor.core.publisher.Mono;

public interface TokenGenerator {
    Mono<String> generate(Usuario usuario);
}
