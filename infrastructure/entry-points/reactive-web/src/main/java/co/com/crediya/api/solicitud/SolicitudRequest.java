package co.com.crediya.api.solicitud;

import co.com.crediya.model.solicitud.Solicitud;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record SolicitudRequest(
        @Schema(example = "1020304050", requiredMode = Schema.RequiredMode.REQUIRED) @JsonProperty("documento_identidad") String documentoIdentidad,
        @Schema(example = "5000000", requiredMode = Schema.RequiredMode.REQUIRED) @JsonProperty("monto") BigDecimal monto,
        @Schema(example = "24", requiredMode = Schema.RequiredMode.REQUIRED) @JsonProperty("plazo") Integer plazo,
        @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) @JsonProperty("id_tipo_prestamo") Long idTipoPrestamo) {

    public Solicitud toDomain() {
        return Solicitud.builder()
                .monto(monto)
                .plazo(plazo)
                .idTipoPrestamo(idTipoPrestamo)
                .build();
    }
}
