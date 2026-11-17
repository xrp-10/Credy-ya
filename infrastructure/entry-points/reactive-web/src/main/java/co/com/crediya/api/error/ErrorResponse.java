package co.com.crediya.api.error;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Cuerpo de toda respuesta de error de la API. */
public record ErrorResponse(
        @JsonProperty("estado") int estado,
        @JsonProperty("error") String error,
        @JsonProperty("mensaje") String mensaje) {
}
