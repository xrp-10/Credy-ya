package co.com.crediya.model;

import co.com.crediya.model.exceptions.BusinessErrorType;
import co.com.crediya.model.exceptions.BusinessException;
import co.com.crediya.model.pagina.Pagina;
import co.com.crediya.model.solicitud.EstadoSolicitud;
import co.com.crediya.model.usuario.Rol;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModeloTest {

    @Test
    void losRolesCoincidenConLaTablaRol() {
        assertThat(Rol.desdeId(1)).contains(Rol.ADMIN);
        assertThat(Rol.desdeId(2)).contains(Rol.ASESOR);
        assertThat(Rol.desdeId(3)).contains(Rol.CLIENTE);
        assertThat(Rol.desdeId(4)).isEmpty();
        assertThat(Rol.desdeId(null)).isEmpty();
    }

    @Test
    void losEstadosCoincidenConLaTablaEstados() {
        assertThat(EstadoSolicitud.desdeId(1)).contains(EstadoSolicitud.PENDIENTE_REVISION);
        assertThat(EstadoSolicitud.PENDIENTE_REVISION.getNombre()).isEqualTo("Pendiente de revisión");
        assertThat(EstadoSolicitud.desdeId(4)).contains(EstadoSolicitud.APROBADA);
        assertThat(EstadoSolicitud.desdeId(5)).isEmpty();
        assertThat(EstadoSolicitud.desdeId(null)).isEmpty();
    }

    @Test
    void laPaginaCalculaElTotalDePaginasYEsInmutable() {
        List<String> datos = new ArrayList<>(List.of("a", "b"));
        Pagina<String> pagina = Pagina.de(datos, 1, 2, 5);
        datos.clear();

        assertThat(pagina.contenido()).containsExactly("a", "b");
        assertThat(pagina.totalPaginas()).isEqualTo(3);
        assertThat(Pagina.de(List.of(), 0, 10, 0).totalPaginas()).isZero();
        assertThat(Pagina.de(List.of(), 0, 10, 10).totalPaginas()).isEqualTo(1);
        assertThat(Pagina.de(List.of(), 0, 10, 11).totalPaginas()).isEqualTo(2);
        assertThatThrownBy(() -> pagina.contenido().add("c")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void laExcepcionDeNegocioGuardaSuTipoYMensaje() {
        BusinessException error = new BusinessException(BusinessErrorType.NOT_FOUND, "no existe");

        assertThat(error.getType()).isEqualTo(BusinessErrorType.NOT_FOUND);
        assertThat(error.getMessage()).isEqualTo("no existe");
    }
}
