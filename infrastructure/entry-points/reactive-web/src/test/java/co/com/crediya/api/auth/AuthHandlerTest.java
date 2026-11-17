package co.com.crediya.api.auth;

import co.com.crediya.api.RouterRest;
import co.com.crediya.api.error.ErrorResponseWriter;
import co.com.crediya.api.error.GlobalErrorHandler;
import co.com.crediya.model.exceptions.BusinessErrorType;
import co.com.crediya.model.exceptions.BusinessException;
import co.com.crediya.usecase.login.LoginUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthHandlerTest {

    @Mock
    private LoginUseCase loginUseCase;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        GlobalErrorHandler errores = new GlobalErrorHandler(new ErrorResponseWriter(ServerCodecConfigurer.create()));
        client = WebTestClient.bindToRouterFunction(new RouterRest().authRoutes(new AuthHandler(loginUseCase)))
                .handlerStrategies(HandlerStrategies.empty()
                        .codecs(codecs -> codecs.registerDefaults(true))
                        .exceptionHandler(errores).build())
                .build();
    }

    @Test
    void devuelveElTokenBearer() {
        when(loginUseCase.login("ana@correo.com", "ClaveSegura1")).thenReturn(Mono.just("jwt-firmado"));

        client.post().uri(RouterRest.LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"correo_electronico\": \"ana@correo.com\", \"clave\": \"ClaveSegura1\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.token").isEqualTo("jwt-firmado")
                .jsonPath("$.tipo").isEqualTo("Bearer");
    }

    @Test
    void credencialesIncorrectasYCuerpoVacioSeRespondenEnJson() {
        when(loginUseCase.login("ana@correo.com", "Mala12345")).thenReturn(Mono.error(
                new BusinessException(BusinessErrorType.UNAUTHORIZED, "Correo o clave incorrectos")));

        client.post().uri(RouterRest.LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"correo_electronico\": \"ana@correo.com\", \"clave\": \"Mala12345\"}")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.mensaje").isEqualTo("Correo o clave incorrectos");
        client.post().uri(RouterRest.LOGIN).contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest();
    }
}
