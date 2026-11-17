package co.com.crediya.security;

import co.com.crediya.model.auth.gateways.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Cifra y verifica claves con BCrypt. BCrypt es lento a propósito, así que se ejecuta
 * fuera del hilo de eventos de WebFlux.
 */
@Component
public class BCryptPasswordHasher implements PasswordHasher {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public Mono<String> hash(String clave) {
        return Mono.fromCallable(() -> encoder.encode(clave))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Boolean> matches(String clave, String hash) {
        return Mono.fromCallable(() -> encoder.matches(clave, hash))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
