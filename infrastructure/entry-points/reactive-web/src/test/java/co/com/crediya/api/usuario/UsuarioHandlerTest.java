package co.com.crediya.api.usuario;

import co.com.crediya.api.RouterRest;
import co.com.crediya.api.error.ErrorResponseWriter;
import co.com.crediya.api.error.GlobalErrorHandler;
import co.com.crediya.model.usuario.Usuario;
import co.com.crediya.usecase.usuario.UsuarioUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioHandlerTest {

    @Mock
    private UsuarioUseCase usuarioUseCase;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        UsuarioHandler handler = new UsuarioHandler(usuarioUseCase);
        GlobalErrorHandler errores = new GlobalErrorHandler(new ErrorResponseWriter(ServerCodecConfigurer.create()));
        client = WebTestClient.bindToRouterFunction(new RouterRest().routerFunction(handler))
                .handlerStrategies(HandlerStrategies.empty()
                        .codecs(codecs -> codecs.registerDefaults(true))
                        .exceptionHandler(errores).build())
                .build();
    }

    @Test
    void registraUsuarioYDevuelve201SinLaClave() {
        when(usuarioUseCase.registrar(any())).thenAnswer(i -> Mono.just(((Usuario) i.getArgument(0))
                .toBuilder().idUsuario(7L).contrasena("hash").idRol(3).build()));

        client.post().uri(RouterRest.USUARIOS)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"nombres": "Ana", "apellidos": "Pérez", "fecha_nacimiento": "1995-04-10",
                         "direccion": "Calle 10", "telefono": "3001234567",
                         "correo_electronico": "ana@correo.com", "salario_base": 2500000,
                         "documento_identidad": "1020304050", "clave": "ClaveSegura1"}
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().location("/api/v1/usuarios/7")
                .expectBody()
                .jsonPath("$.id_usuario").isEqualTo(7)
                .jsonPath("$.nombres").isEqualTo("Ana")
                .jsonPath("$.correo_electronico").isEqualTo("ana@correo.com")
                .jsonPath("$.fecha_nacimiento").isEqualTo("1995-04-10")
                .jsonPath("$.id_rol").isEqualTo(3)
                .jsonPath("$.clave").doesNotExist()
                .jsonPath("$.contrasena").doesNotExist();

        ArgumentCaptor<Usuario> enviado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioUseCase).registrar(enviado.capture());
        assertThat(enviado.getValue().getNombre()).isEqualTo("Ana");
        assertThat(enviado.getValue().getApellido()).isEqualTo("Pérez");
        assertThat(enviado.getValue().getEmail()).isEqualTo("ana@correo.com");
        assertThat(enviado.getValue().getSalarioBase()).isEqualByComparingTo(new BigDecimal("2500000"));
        assertThat(enviado.getValue().getFechaNacimiento()).isEqualTo(LocalDate.of(1995, 4, 10));
        assertThat(enviado.getValue().getDocumentoIdentidad()).isEqualTo("1020304050");
        assertThat(enviado.getValue().getContrasena()).isEqualTo("ClaveSegura1");
        assertThat(enviado.getValue().getDireccion()).isEqualTo("Calle 10");
        assertThat(enviado.getValue().getTelefono()).isEqualTo("3001234567");
    }

    @Test
    void cuerpoVacioOSalarioNoNumericoDevuelven400() {
        client.post().uri(RouterRest.USUARIOS)
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.mensaje").isEqualTo("El cuerpo de la petición es obligatorio");
        client.post().uri(RouterRest.USUARIOS)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"nombres\": \"Ana\", \"salario_base\": \"mucho\"}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.error").isEqualTo("SOLICITUD_INVALIDA");
        verify(usuarioUseCase, never()).registrar(any());
    }
}
