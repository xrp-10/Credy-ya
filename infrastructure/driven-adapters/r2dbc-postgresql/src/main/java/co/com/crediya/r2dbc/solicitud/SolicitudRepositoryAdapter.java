package co.com.crediya.r2dbc.solicitud;

import co.com.crediya.model.exceptions.BusinessErrorType;
import co.com.crediya.model.exceptions.BusinessException;
import co.com.crediya.model.solicitud.Solicitud;
import co.com.crediya.model.solicitud.gateways.SolicitudRepository;
import co.com.crediya.r2dbc.helper.ReactiveAdapterOperations;
import lombok.extern.slf4j.Slf4j;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

@Slf4j
@Repository
public class SolicitudRepositoryAdapter extends ReactiveAdapterOperations<
        Solicitud,
        SolicitudEntity,
        Long,
        SolicitudReactiveRepository
        > implements SolicitudRepository {

    private final TransactionalOperator transaccion;

    public SolicitudRepositoryAdapter(SolicitudReactiveRepository repository, ObjectMapper mapper,
                                      TransactionalOperator transaccion) {
        super(repository, mapper, entity -> mapper.map(entity, Solicitud.class));
        this.transaccion = transaccion;
    }

    /** Guarda la solicitud dentro de una transacción reactiva: o queda completa o no queda nada. */
    @Override
    public Mono<Solicitud> save(Solicitud solicitud) {
        return super.save(solicitud)
                .as(transaccion::transactional)
                .doOnSubscribe(s -> log.debug("Guardando solicitud en la base de datos"))
                .doOnSuccess(guardada -> log.info("Solicitud guardada con id {}", guardada.getIdSolicitud()))
                .onErrorMap(DataIntegrityViolationException.class, e -> new BusinessException(
                        BusinessErrorType.INVALID_DATA, "Los datos de la solicitud no cumplen las reglas de la base de datos"))
                .doOnError(e -> log.warn("No se pudo guardar la solicitud: {}", e.getMessage()));
    }
}
