package co.com.crediya.model.solicitudrevision;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Fila del listado que revisa el asesor (HU4). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class SolicitudRevision {
    private Long idSolicitud;
    private BigDecimal monto;
    private Integer plazo;
    private String email;
    private String nombre;
    private String tipoPrestamo;
    private BigDecimal tasaInteres;
    private String estadoSolicitud;
    private BigDecimal salarioBase;
    private BigDecimal deudaTotalMensualSolicitudesAprobadas;
}
