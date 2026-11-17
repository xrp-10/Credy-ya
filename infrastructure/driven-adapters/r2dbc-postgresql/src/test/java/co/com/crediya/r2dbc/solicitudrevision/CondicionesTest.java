package co.com.crediya.r2dbc.solicitudrevision;

import co.com.crediya.model.solicitudrevision.FiltroRevision;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CondicionesTest {

    @Test
    void sinFiltrosSoloLimitaPorEstados() {
        SolicitudRevisionRepositoryAdapter.Condiciones condiciones = SolicitudRevisionRepositoryAdapter.Condiciones
                .de(FiltroRevision.builder().estados(List.of(1, 2, 3)).build());

        assertThat(condiciones.where()).isEqualTo(" WHERE s.id_estado IN (:estados)");
        assertThat(condiciones.parametros()).containsOnlyKeys("estados").containsEntry("estados", List.of(1, 2, 3));
    }

    @Test
    void agregaCorreoYTipoComoParametros() {
        SolicitudRevisionRepositoryAdapter.Condiciones condiciones = SolicitudRevisionRepositoryAdapter.Condiciones
                .de(FiltroRevision.builder().estados(List.of(3)).email("a@crediya.test").idTipoPrestamo(2L).build());

        assertThat(condiciones.where())
                .isEqualTo(" WHERE s.id_estado IN (:estados) AND s.email = :email AND s.id_tipo_prestamo = :tipo");
        assertThat(condiciones.parametros())
                .containsEntry("email", "a@crediya.test")
                .containsEntry("tipo", 2L)
                .containsEntry("estados", List.of(3));
    }

    @Test
    void elCorreoNuncaQuedaEnElTextoDeLaConsulta() {
        SolicitudRevisionRepositoryAdapter.Condiciones condiciones = SolicitudRevisionRepositoryAdapter.Condiciones
                .de(FiltroRevision.builder().estados(List.of(1)).email("x' OR '1'='1").build());

        assertThat(condiciones.where()).doesNotContain("OR '1'='1");
    }
}
