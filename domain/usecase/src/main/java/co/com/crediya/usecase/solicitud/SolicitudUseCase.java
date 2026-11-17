package co.com.crediya.usecase.solicitud;

import co.com.crediya.model.exceptions.BusinessErrorType;
import co.com.crediya.model.exceptions.BusinessException;
import co.com.crediya.model.solicitud.EstadoSolicitud;
import co.com.crediya.model.solicitud.Solicitud;
import co.com.crediya.model.solicitud.gateways.SolicitudRepository;
import co.com.crediya.model.tipoprestamo.TipoPrestamo;
import co.com.crediya.model.tipoprestamo.gateways.TipoPrestamoRepository;
import co.com.crediya.model.usuario.Usuario;
import co.com.crediya.model.usuario.gateways.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@RequiredArgsConstructor
public class SolicitudUseCase {

    private final SolicitudRepository solicitudRepository;
    private final TipoPrestamoRepository tipoPrestamoRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Registra la solicitud del cliente dueño del documento. Quien la crea (emailSolicitante, tomado del token)
     * solo puede hacerlo a su propio nombre (HU3).
     */
    public Mono<Solicitud> registrar(Solicitud solicitud, String documentoIdentidad, String emailSolicitante) {
        return validar(solicitud, documentoIdentidad)
                .then(Mono.defer(() -> buscarCliente(documentoIdentidad.trim())))
                .flatMap(cliente -> exigirQueSeaElMismo(cliente, emailSolicitante))
                .zipWhen(cliente -> buscarTipoPrestamo(solicitud.getIdTipoPrestamo()))
                .flatMap(datos -> validarMonto(solicitud.getMonto(), datos.getT2())
                        .thenReturn(nuevaSolicitud(solicitud, datos.getT1())))
                .flatMap(solicitudRepository::save);
    }

    private static Mono<Void> validar(Solicitud solicitud, String documentoIdentidad) {
        if (solicitud == null) {
            return invalido("Los datos de la solicitud son obligatorios");
        }
        if (documentoIdentidad == null || documentoIdentidad.isBlank()) {
            return invalido("El campo documento_identidad es obligatorio");
        }
        if (solicitud.getMonto() == null || solicitud.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            return invalido("El monto es obligatorio y debe ser mayor que 0");
        }
        if (solicitud.getPlazo() == null || solicitud.getPlazo() <= 0) {
            return invalido("El plazo es obligatorio y debe ser de al menos 1 mes");
        }
        if (solicitud.getIdTipoPrestamo() == null) {
            return invalido("El campo id_tipo_prestamo es obligatorio");
        }
        return Mono.empty();
    }

    private Mono<Usuario> buscarCliente(String documentoIdentidad) {
        return usuarioRepository.findByDocumentoIdentidad(documentoIdentidad)
                .switchIfEmpty(Mono.error(new BusinessException(BusinessErrorType.NOT_FOUND,
                        "No existe un cliente con ese documento de identidad")));
    }

    private static Mono<Usuario> exigirQueSeaElMismo(Usuario cliente, String emailSolicitante) {
        if (emailSolicitante == null || !cliente.getEmail().equalsIgnoreCase(emailSolicitante.trim())) {
            return Mono.error(new BusinessException(BusinessErrorType.FORBIDDEN,
                    "Solo puedes crear solicitudes de préstamo a tu nombre"));
        }
        return Mono.just(cliente);
    }

    private Mono<TipoPrestamo> buscarTipoPrestamo(Long idTipoPrestamo) {
        return tipoPrestamoRepository.findById(idTipoPrestamo)
                .switchIfEmpty(invalido("El tipo de préstamo seleccionado no existe"));
    }

    private static Mono<Void> validarMonto(BigDecimal monto, TipoPrestamo tipo) {
        if (monto.compareTo(tipo.getMontoMinimo()) < 0 || monto.compareTo(tipo.getMontoMaximo()) > 0) {
            return invalido("Para " + tipo.getNombre() + " el monto debe estar entre "
                    + tipo.getMontoMinimo().toPlainString() + " y " + tipo.getMontoMaximo().toPlainString());
        }
        return Mono.empty();
    }

    private static Solicitud nuevaSolicitud(Solicitud solicitud, Usuario cliente) {
        return solicitud.toBuilder()
                .idSolicitud(null)
                .email(cliente.getEmail())
                .idEstado(EstadoSolicitud.PENDIENTE_REVISION.getId())
                .build();
    }

    private static <T> Mono<T> invalido(String mensaje) {
        return Mono.error(new BusinessException(BusinessErrorType.INVALID_DATA, mensaje));
    }
}
