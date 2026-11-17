package co.com.crediya.model.solicitudrevision.gateways;

import co.com.crediya.model.solicitudrevision.FiltroRevision;
import co.com.crediya.model.solicitudrevision.SolicitudRevision;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SolicitudRevisionRepository {
    Flux<SolicitudRevision> findForReview(FiltroRevision filtro);

    Mono<Long> countForReview(FiltroRevision filtro);
}
