package co.com.crediya.api;

import co.com.crediya.api.auth.AuthHandler;
import co.com.crediya.api.auth.LoginRequest;
import co.com.crediya.api.auth.TokenResponse;
import co.com.crediya.api.config.OpenApiConfig;
import co.com.crediya.api.error.ErrorResponse;
import co.com.crediya.api.revision.PaginaResponse;
import co.com.crediya.api.revision.RevisionHandler;
import co.com.crediya.api.solicitud.SolicitudHandler;
import co.com.crediya.api.solicitud.SolicitudRequest;
import co.com.crediya.api.solicitud.SolicitudResponse;
import co.com.crediya.api.usuario.UsuarioHandler;
import co.com.crediya.api.usuario.UsuarioRequest;
import co.com.crediya.api.usuario.UsuarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springdoc.core.annotations.RouterOperation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterRest {

    public static final String USUARIOS = "/api/v1/usuarios";
    public static final String SOLICITUD = "/api/v1/solicitud";
    public static final String LOGIN = "/api/v1/login";

    @Bean
    @RouterOperation(path = USUARIOS, method = RequestMethod.POST, beanClass = UsuarioHandler.class, beanMethod = "registrar",
            operation = @Operation(operationId = "registrarUsuario", tags = "HU1 Usuarios",
                    summary = "Registrar un usuario (solo ADMIN o ASESOR)",
                    security = @SecurityRequirement(name = OpenApiConfig.BEARER),
                    requestBody = @RequestBody(required = true,
                            content = @Content(schema = @Schema(implementation = UsuarioRequest.class))),
                    responses = {
                            @ApiResponse(responseCode = "201", description = "Usuario registrado",
                                    content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
                            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                            @ApiResponse(responseCode = "401", description = "Sin sesión o token inválido",
                                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                            @ApiResponse(responseCode = "403", description = "Tu rol no puede registrar usuarios",
                                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                            @ApiResponse(responseCode = "409", description = "Correo o documento ya registrados",
                                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))}))
    public RouterFunction<ServerResponse> usuarioRoutes(UsuarioHandler handler) {
        return route(POST(USUARIOS), handler::registrar);
    }

    @Bean
    @RouterOperation(path = SOLICITUD, method = RequestMethod.POST, beanClass = SolicitudHandler.class, beanMethod = "registrar",
            operation = @Operation(operationId = "registrarSolicitud", tags = "HU2 Solicitudes",
                    summary = "Registrar una solicitud de préstamo a tu nombre (solo CLIENTE)",
                    security = @SecurityRequirement(name = OpenApiConfig.BEARER),
                    requestBody = @RequestBody(required = true,
                            content = @Content(schema = @Schema(implementation = SolicitudRequest.class))),
                    responses = {
                            @ApiResponse(responseCode = "201", description = "Solicitud registrada en \"Pendiente de revisión\"",
                                    content = @Content(schema = @Schema(implementation = SolicitudResponse.class))),
                            @ApiResponse(responseCode = "400", description = "Datos inválidos, tipo inexistente o monto fuera de rango",
                                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                            @ApiResponse(responseCode = "401", description = "Sin sesión o token inválido",
                                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                            @ApiResponse(responseCode = "403", description = "No eres CLIENTE o el documento no es tuyo",
                                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                            @ApiResponse(responseCode = "404", description = "No existe un cliente con ese documento",
                                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))}))
    public RouterFunction<ServerResponse> solicitudRoutes(SolicitudHandler handler) {
        return route(POST(SOLICITUD), handler::registrar);
    }

    @Bean
    @RouterOperation(path = LOGIN, method = RequestMethod.POST, beanClass = AuthHandler.class, beanMethod = "login",
            operation = @Operation(operationId = "login", tags = "HU3 Autenticación",
                    summary = "Iniciar sesión con correo y clave",
                    requestBody = @RequestBody(required = true,
                            content = @Content(schema = @Schema(implementation = LoginRequest.class))),
                    responses = {
                            @ApiResponse(responseCode = "200", description = "Token JWT para el botón Authorize",
                                    content = @Content(schema = @Schema(implementation = TokenResponse.class))),
                            @ApiResponse(responseCode = "400", description = "Falta el correo o la clave",
                                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                            @ApiResponse(responseCode = "401", description = "Correo o clave incorrectos",
                                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))}))
    public RouterFunction<ServerResponse> authRoutes(AuthHandler handler) {
        return route(POST(LOGIN), handler::login);
    }

    @Bean
    @RouterOperation(path = SOLICITUD, method = RequestMethod.GET, beanClass = RevisionHandler.class, beanMethod = "listar",
            operation = @Operation(operationId = "listarSolicitudesEnRevision", tags = "HU4 Revisión",
                    summary = "Listar solicitudes pendientes, rechazadas y en revisión manual (solo ASESOR)",
                    security = @SecurityRequirement(name = OpenApiConfig.BEARER),
                    parameters = {
                            @Parameter(in = ParameterIn.QUERY, name = "estado",
                                    description = "1 Pendiente de revisión, 2 Rechazada o 3 Revisión manual",
                                    schema = @Schema(type = "integer")),
                            @Parameter(in = ParameterIn.QUERY, name = "email", description = "Correo del cliente",
                                    schema = @Schema(type = "string")),
                            @Parameter(in = ParameterIn.QUERY, name = "tipo_prestamo", description = "Id del tipo de préstamo",
                                    schema = @Schema(type = "integer")),
                            @Parameter(in = ParameterIn.QUERY, name = "pagina", description = "Desde 0",
                                    schema = @Schema(type = "integer", defaultValue = "0")),
                            @Parameter(in = ParameterIn.QUERY, name = "tamano", description = "De 1 a 50",
                                    schema = @Schema(type = "integer", defaultValue = "10"))},
                    responses = {
                            @ApiResponse(responseCode = "200", description = "Página de solicitudes",
                                    content = @Content(schema = @Schema(implementation = PaginaResponse.class))),
                            @ApiResponse(responseCode = "400", description = "Parámetros inválidos",
                                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                            @ApiResponse(responseCode = "401", description = "Sin sesión o token inválido",
                                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                            @ApiResponse(responseCode = "403", description = "Solo el ASESOR puede ver el listado",
                                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))}))
    public RouterFunction<ServerResponse> revisionRoutes(RevisionHandler handler) {
        return route(GET(SOLICITUD), handler::listar);
    }
}
