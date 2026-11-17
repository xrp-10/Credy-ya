package co.com.crediya.model.auth.gateways;

import reactor.core.publisher.Mono;

public interface PasswordHasher {
    Mono<String> hash(String clave);

    Mono<Boolean> matches(String clave, String hash);
}
