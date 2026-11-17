package co.com.crediya.api.usuario;

import co.com.crediya.model.usuario.Usuario;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Datos del usuario que devuelve la API. Nunca incluye la clave. */
public record UsuarioResponse(
        @JsonProperty("id_usuario") Long idUsuario,
        @JsonProperty("nombres") String nombres,
        @JsonProperty("apellidos") String apellidos,
        @JsonProperty("fecha_nacimiento") LocalDate fechaNacimiento,
        @JsonProperty("direccion") String direccion,
        @JsonProperty("telefono") String telefono,
        @JsonProperty("correo_electronico") String correoElectronico,
        @JsonProperty("salario_base") BigDecimal salarioBase,
        @JsonProperty("documento_identidad") String documentoIdentidad,
        @JsonProperty("id_rol") Integer idRol) {

    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getIdUsuario(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getFechaNacimiento(),
                usuario.getDireccion(),
                usuario.getTelefono(),
                usuario.getEmail(),
                usuario.getSalarioBase(),
                usuario.getDocumentoIdentidad(),
                usuario.getIdRol());
    }
}
