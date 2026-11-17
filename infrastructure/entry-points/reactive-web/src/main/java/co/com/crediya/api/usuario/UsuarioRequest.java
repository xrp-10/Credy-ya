package co.com.crediya.api.usuario;

import co.com.crediya.model.usuario.Usuario;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UsuarioRequest(
        @Schema(example = "Ana María", requiredMode = Schema.RequiredMode.REQUIRED) @JsonProperty("nombres") String nombres,
        @Schema(example = "Pérez Gómez", requiredMode = Schema.RequiredMode.REQUIRED) @JsonProperty("apellidos") String apellidos,
        @Schema(example = "1995-04-10") @JsonProperty("fecha_nacimiento") LocalDate fechaNacimiento,
        @Schema(example = "Calle 10 # 5-20") @JsonProperty("direccion") String direccion,
        @Schema(example = "3001234567") @JsonProperty("telefono") String telefono,
        @Schema(example = "ana.perez@correo.com", requiredMode = Schema.RequiredMode.REQUIRED) @JsonProperty("correo_electronico") String correoElectronico,
        @Schema(example = "2500000", requiredMode = Schema.RequiredMode.REQUIRED) @JsonProperty("salario_base") BigDecimal salarioBase,
        @Schema(example = "1020304050", requiredMode = Schema.RequiredMode.REQUIRED) @JsonProperty("documento_identidad") String documentoIdentidad,
        @Schema(example = "ClaveSegura1", requiredMode = Schema.RequiredMode.REQUIRED) @JsonProperty("clave") String clave,
        @Schema(example = "3") @JsonProperty("id_rol") Integer idRol) {

    public Usuario toDomain() {
        return Usuario.builder()
                .nombre(nombres)
                .apellido(apellidos)
                .fechaNacimiento(fechaNacimiento)
                .direccion(direccion)
                .telefono(telefono)
                .email(correoElectronico)
                .salarioBase(salarioBase)
                .documentoIdentidad(documentoIdentidad)
                .contrasena(clave)
                .idRol(idRol)
                .build();
    }
}
