package co.com.crediya.model.solicitudrevision;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** Filtros y página del listado de revisión. Los campos nulos no filtran. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class FiltroRevision {
    private List<Integer> estados;
    private String email;
    private Long idTipoPrestamo;
    private Integer pagina;
    private Integer tamano;
}
