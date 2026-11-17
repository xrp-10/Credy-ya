package co.com.crediya.api.revision;

import co.com.crediya.model.solicitudrevision.SolicitudRevision;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record SolicitudRevisionResponse(
        @JsonProperty("id_solicitud") Long idSolicitud,
        @JsonProperty("monto") BigDecimal monto,
        @JsonProperty("plazo") Integer plazo,
        @JsonProperty("email") String email,
        @JsonProperty("nombre") String nombre,
        @JsonProperty("tipo_prestamo") String tipoPrestamo,
        @JsonProperty("tasa_interes") BigDecimal tasaInteres,
        @JsonProperty("estado_solicitud") String estadoSolicitud,
        @JsonProperty("salario_base") BigDecimal salarioBase,
        @JsonProperty("deuda_total_mensual_solicitudes_aprobadas") BigDecimal deudaTotalMensualSolicitudesAprobadas) {

    public static SolicitudRevisionResponse from(SolicitudRevision fila) {
        return new SolicitudRevisionResponse(
                fila.getIdSolicitud(),
                fila.getMonto(),
                fila.getPlazo(),
                fila.getEmail(),
                fila.getNombre(),
                fila.getTipoPrestamo(),
                fila.getTasaInteres(),
                fila.getEstadoSolicitud(),
                fila.getSalarioBase(),
                fila.getDeudaTotalMensualSolicitudesAprobadas());
    }
}
