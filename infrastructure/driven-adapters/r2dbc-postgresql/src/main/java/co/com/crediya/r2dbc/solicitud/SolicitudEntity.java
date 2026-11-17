package co.com.crediya.r2dbc.solicitud;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table("solicitud")
public class SolicitudEntity {
    @Id
    private Long idSolicitud;
    private BigDecimal monto;
    private Integer plazo;
    private String email;
    private Integer idEstado;
    private Long idTipoPrestamo;
}
