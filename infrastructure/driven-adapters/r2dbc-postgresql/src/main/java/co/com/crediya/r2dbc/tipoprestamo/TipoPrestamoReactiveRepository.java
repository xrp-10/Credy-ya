package co.com.crediya.r2dbc.tipoprestamo;

import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface TipoPrestamoReactiveRepository extends ReactiveCrudRepository<TipoPrestamoEntity, Long>,
        ReactiveQueryByExampleExecutor<TipoPrestamoEntity> {
}
