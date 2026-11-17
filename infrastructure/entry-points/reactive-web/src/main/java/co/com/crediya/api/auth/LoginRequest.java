package co.com.crediya.api.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

public record LoginRequest(
        @Schema(example = "ana.perez@correo.com", requiredMode = Schema.RequiredMode.REQUIRED) @JsonProperty("correo_electronico") String correoElectronico,
        @Schema(example = "ClaveSegura1", requiredMode = Schema.RequiredMode.REQUIRED) @JsonProperty("clave") String clave) {
}
