package co.com.crediya.api.solicitud;

import co.com.crediya.model.solicitud.Solicitud;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record SolicitudRequest(
        @JsonProperty("documento_identidad") String documentoIdentidad,
        @JsonProperty("monto") BigDecimal monto,
        @JsonProperty("plazo") Integer plazo,
        @JsonProperty("id_tipo_prestamo") Long idTipoPrestamo) {

    public Solicitud toDomain() {
        return Solicitud.builder()
                .monto(monto)
                .plazo(plazo)
                .idTipoPrestamo(idTipoPrestamo)
                .build();
    }
}
