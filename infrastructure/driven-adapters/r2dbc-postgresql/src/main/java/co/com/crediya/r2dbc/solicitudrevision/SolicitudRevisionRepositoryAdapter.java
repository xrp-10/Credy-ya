package co.com.crediya.r2dbc.solicitudrevision;

import co.com.crediya.model.solicitud.EstadoSolicitud;
import co.com.crediya.model.solicitudrevision.FiltroRevision;
import co.com.crediya.model.solicitudrevision.SolicitudRevision;
import co.com.crediya.model.solicitudrevision.gateways.SolicitudRevisionRepository;
import io.r2dbc.spi.Readable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/** Consulta del listado de revisión (HU4): una sola consulta une solicitud, tipo, estado y cliente. */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SolicitudRevisionRepositoryAdapter implements SolicitudRevisionRepository {

    /**
     * Suma de las cuotas mensuales de las solicitudes aprobadas del mismo cliente.
     * Cuota fija: monto * i / (1 - (1 + i)^-plazo), con i = tasa mensual (tasa_interes / 100).
     */
    private static final String DEUDA_MENSUAL_APROBADAS = """
            COALESCE((SELECT SUM(CASE WHEN ta.tasa_interes = 0 THEN sa.monto / sa.plazo
                                      ELSE sa.monto * (ta.tasa_interes / 100)
                                           / (1 - POWER(1 + ta.tasa_interes / 100, -sa.plazo)) END)
                      FROM solicitud sa
                      JOIN tipo_prestamo ta ON ta.id_tipo_prestamo = sa.id_tipo_prestamo
                      WHERE sa.email = s.email AND sa.id_estado = :aprobada), 0)""";

    private static final String SELECT_LISTADO = """
            SELECT s.id_solicitud, s.monto, s.plazo, s.email,
                   NULLIF(TRIM(CONCAT(u.nombre, ' ', u.apellido)), '') AS nombre,
                   tp.nombre AS tipo_prestamo, tp.tasa_interes, e.nombre AS estado_solicitud,
                   u.salario_base, ROUND(%s, 2) AS deuda_total_mensual
            FROM solicitud s
            JOIN tipo_prestamo tp ON tp.id_tipo_prestamo = s.id_tipo_prestamo
            JOIN estados e ON e.id_estado = s.id_estado
            LEFT JOIN usuario u ON u.email = s.email""".formatted(DEUDA_MENSUAL_APROBADAS);

    private final DatabaseClient databaseClient;

    @Override
    public Flux<SolicitudRevision> findForReview(FiltroRevision filtro) {
        Condiciones condiciones = Condiciones.de(filtro);
        String sql = SELECT_LISTADO + condiciones.where() + " ORDER BY s.id_solicitud LIMIT :limite OFFSET :desde";
        DatabaseClient.GenericExecuteSpec consulta = databaseClient.sql(sql)
                .bind("aprobada", EstadoSolicitud.APROBADA.getId())
                .bind("limite", filtro.getTamano())
                .bind("desde", (long) filtro.getPagina() * filtro.getTamano());
        return condiciones.enlazar(consulta)
                .map(SolicitudRevisionRepositoryAdapter::aFila)
                .all()
                .doOnSubscribe(s -> log.debug("Consultando solicitudes en revisión, página {}", filtro.getPagina()))
                .doOnError(e -> log.error("Falló la consulta del listado de revisión: {}", e.getMessage()));
    }

    @Override
    public Mono<Long> countForReview(FiltroRevision filtro) {
        Condiciones condiciones = Condiciones.de(filtro);
        return condiciones.enlazar(databaseClient.sql("SELECT COUNT(*) AS total FROM solicitud s" + condiciones.where()))
                .map(row -> row.get("total", Long.class))
                .one();
    }

    private static SolicitudRevision aFila(Readable row) {
        return SolicitudRevision.builder()
                .idSolicitud(row.get("id_solicitud", Long.class))
                .monto(row.get("monto", BigDecimal.class))
                .plazo(row.get("plazo", Integer.class))
                .email(row.get("email", String.class))
                .nombre(row.get("nombre", String.class))
                .tipoPrestamo(row.get("tipo_prestamo", String.class))
                .tasaInteres(row.get("tasa_interes", BigDecimal.class))
                .estadoSolicitud(row.get("estado_solicitud", String.class))
                .salarioBase(row.get("salario_base", BigDecimal.class))
                .deudaTotalMensualSolicitudesAprobadas(row.get("deuda_total_mensual", BigDecimal.class))
                .build();
    }

    /** WHERE y parámetros según los filtros presentes; los valores siempre van como parámetros, nunca en el texto. */
    record Condiciones(String where, Map<String, Object> parametros) {

        static Condiciones de(FiltroRevision filtro) {
            StringBuilder where = new StringBuilder(" WHERE s.id_estado IN (:estados)");
            Map<String, Object> parametros = new LinkedHashMap<>();
            parametros.put("estados", filtro.getEstados());
            if (filtro.getEmail() != null) {
                where.append(" AND s.email = :email");
                parametros.put("email", filtro.getEmail());
            }
            if (filtro.getIdTipoPrestamo() != null) {
                where.append(" AND s.id_tipo_prestamo = :tipo");
                parametros.put("tipo", filtro.getIdTipoPrestamo());
            }
            return new Condiciones(where.toString(), parametros);
        }

        DatabaseClient.GenericExecuteSpec enlazar(DatabaseClient.GenericExecuteSpec consulta) {
            DatabaseClient.GenericExecuteSpec resultado = consulta;
            for (Map.Entry<String, Object> parametro : parametros.entrySet()) {
                resultado = resultado.bind(parametro.getKey(), parametro.getValue());
            }
            return resultado;
        }
    }
}
