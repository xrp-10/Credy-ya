package co.com.crediya.model.solicitud;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

/** Estados de una solicitud. Los ids coinciden con la tabla estados. */
@Getter
@RequiredArgsConstructor
public enum EstadoSolicitud {
    PENDIENTE_REVISION(1, "Pendiente de revisión"),
    RECHAZADA(2, "Rechazada"),
    REVISION_MANUAL(3, "Revisión manual"),
    APROBADA(4, "Aprobada");

    private final int id;
    private final String nombre;

    public static Optional<EstadoSolicitud> desdeId(Integer id) {
        return Arrays.stream(values())
                .filter(estado -> id != null && estado.id == id)
                .findFirst();
    }
}
