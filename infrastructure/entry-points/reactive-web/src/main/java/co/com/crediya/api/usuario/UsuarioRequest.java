package co.com.crediya.api.usuario;

import co.com.crediya.model.usuario.Usuario;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UsuarioRequest(
        @JsonProperty("nombres") String nombres,
        @JsonProperty("apellidos") String apellidos,
        @JsonProperty("fecha_nacimiento") LocalDate fechaNacimiento,
        @JsonProperty("direccion") String direccion,
        @JsonProperty("telefono") String telefono,
        @JsonProperty("correo_electronico") String correoElectronico,
        @JsonProperty("salario_base") BigDecimal salarioBase,
        @JsonProperty("documento_identidad") String documentoIdentidad,
        @JsonProperty("clave") String clave,
        @JsonProperty("id_rol") Integer idRol) {

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
