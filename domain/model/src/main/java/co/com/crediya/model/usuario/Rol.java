package co.com.crediya.model.usuario;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum Rol {
    ADMIN(1),
    ASESOR(2),
    CLIENTE(3);

    private final int id;

    public static Optional<Rol> desdeId(Integer id) {
        return Arrays.stream(values())
                .filter(rol -> id != null && rol.id == id)
                .findFirst();
    }
}
