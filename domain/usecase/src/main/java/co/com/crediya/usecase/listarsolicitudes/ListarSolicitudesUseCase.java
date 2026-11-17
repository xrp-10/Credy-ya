package co.com.crediya.usecase.listarsolicitudes;

import co.com.crediya.model.exceptions.BusinessErrorType;
import co.com.crediya.model.exceptions.BusinessException;
import co.com.crediya.model.pagina.Pagina;
import co.com.crediya.model.solicitud.EstadoSolicitud;
import co.com.crediya.model.solicitudrevision.FiltroRevision;
import co.com.crediya.model.solicitudrevision.SolicitudRevision;
import co.com.crediya.model.solicitudrevision.gateways.SolicitudRevisionRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Locale;

/**
 * Listado paginado y filtrable de las solicitudes que esperan la decisión del asesor (HU4):
 * "Pendiente de revisión", "Rechazada" y "Revisión manual".
 */
@RequiredArgsConstructor
public class ListarSolicitudesUseCase {

    static final List<Integer> ESTADOS_EN_REVISION = List.of(
            EstadoSolicitud.PENDIENTE_REVISION.getId(),
            EstadoSolicitud.RECHAZADA.getId(),
            EstadoSolicitud.REVISION_MANUAL.getId());
    static final int TAMANO_POR_DEFECTO = 10;
    static final int TAMANO_MAXIMO = 50;

    private final SolicitudRevisionRepository solicitudRevisionRepository;

    /** @param idEstado opcional: uno de los estados en revisión para ver solo ese */
    public Mono<Pagina<SolicitudRevision>> listar(Integer idEstado, String email, Long idTipoPrestamo,
                                                  Integer pagina, Integer tamano) {
        int numeroPagina = pagina == null ? 0 : pagina;
        int tamanoPagina = tamano == null ? TAMANO_POR_DEFECTO : tamano;
        if (numeroPagina < 0) {
            return invalido("La pagina debe ser 0 o mayor");
        }
        if (tamanoPagina < 1 || tamanoPagina > TAMANO_MAXIMO) {
            return invalido("El tamano de página debe estar entre 1 y " + TAMANO_MAXIMO);
        }
        if (idEstado != null && !ESTADOS_EN_REVISION.contains(idEstado)) {
            return invalido("El estado debe ser 1 (Pendiente de revisión), 2 (Rechazada) o 3 (Revisión manual)");
        }
        FiltroRevision filtro = FiltroRevision.builder()
                .estados(idEstado == null ? ESTADOS_EN_REVISION : List.of(idEstado))
                .email(email == null || email.isBlank() ? null : email.trim().toLowerCase(Locale.ROOT))
                .idTipoPrestamo(idTipoPrestamo)
                .pagina(numeroPagina)
                .tamano(tamanoPagina)
                .build();
        return Mono.zip(
                        solicitudRevisionRepository.findForReview(filtro).collectList(),
                        solicitudRevisionRepository.countForReview(filtro))
                .map(datos -> Pagina.de(datos.getT1(), numeroPagina, tamanoPagina, datos.getT2()));
    }

    private static <T> Mono<T> invalido(String mensaje) {
        return Mono.error(new BusinessException(BusinessErrorType.INVALID_DATA, mensaje));
    }
}
