package co.com.crediya.api.solicitud;

import co.com.crediya.model.solicitud.EstadoSolicitud;
import co.com.crediya.model.solicitud.Solicitud;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record SolicitudResponse(
        @JsonProperty("id_solicitud") Long idSolicitud,
        @JsonProperty("monto") BigDecimal monto,
        @JsonProperty("plazo") Integer plazo,
        @JsonProperty("email") String email,
        @JsonProperty("id_tipo_prestamo") Long idTipoPrestamo,
        @JsonProperty("estado") String estado) {

    public static SolicitudResponse from(Solicitud solicitud) {
        return new SolicitudResponse(
                solicitud.getIdSolicitud(),
                solicitud.getMonto(),
                solicitud.getPlazo(),
                solicitud.getEmail(),
                solicitud.getIdTipoPrestamo(),
                EstadoSolicitud.desdeId(solicitud.getIdEstado()).map(EstadoSolicitud::getNombre).orElse(null));
    }
}
