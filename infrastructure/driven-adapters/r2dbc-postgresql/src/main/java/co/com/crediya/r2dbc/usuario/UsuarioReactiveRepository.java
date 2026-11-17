package co.com.crediya.r2dbc.usuario;

import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UsuarioReactiveRepository extends ReactiveCrudRepository<UsuarioEntity, Long>,
        ReactiveQueryByExampleExecutor<UsuarioEntity> {

    Mono<Boolean> existsByEmail(String email);

    Mono<Boolean> existsByDocumentoIdentidad(String documentoIdentidad);

    Mono<UsuarioEntity> findByDocumentoIdentidad(String documentoIdentidad);

    Mono<UsuarioEntity> findByEmail(String email);
}
