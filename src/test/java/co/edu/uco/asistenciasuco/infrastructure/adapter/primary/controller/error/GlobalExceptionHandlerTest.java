package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.estudiante.exception.EstudianteErrorCode;
import co.edu.uco.asistenciasuco.application.features.usuario.exception.UsuarioErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.CommonErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private static final String CORRELATION_ID = "93641bab-e3cd-485c-b275-47e7b731e18c";
    private static final String SAFE_INTERNAL_MESSAGE =
            "Ocurrio un error interno. Utilice el codigo de seguimiento para soporte.";

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @AfterEach
    void clearMdc() {
        CorrelationIdContext.clear();
    }

    @Test
    void handleValidation_retorna_badRequest_con_codigo_y_correlationId() {
        CorrelationIdContext.set(java.util.UUID.fromString(CORRELATION_ID));

        final ResponseEntity<ApiErrorResponse> response = handler.handleApplication(
                new ValidationException(UsuarioErrorCode.ERR_CORREO_FORMATO_INVALIDO, "Dato invalido tecnico."),
                request()
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("ERR_CORREO_FORMATO_INVALIDO", response.getBody().code());
        assertEquals("Ingrese un correo valido, por ejemplo nombre@dominio.com.", response.getBody().message());
        assertEquals("/api/prueba", response.getBody().path());
        assertEquals(CORRELATION_ID, response.getBody().correlationId());
    }

    @Test
    void handleConflict_retorna_conflict_sin_exponer_detalles_tecnicos() {
        CorrelationIdContext.set(java.util.UUID.fromString(CORRELATION_ID));

        final ResponseEntity<ApiErrorResponse> response = handler.handleApplication(
                new ConflictException(UsuarioErrorCode.ERR_UNICIDAD_CORREO, "El correo ya existe tecnico."),
                request()
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("ERR_UNICIDAD_CORREO", response.getBody().code());
        assertEquals("El correo ya se encuentra registrado.", response.getBody().message());
        assertEquals(CORRELATION_ID, response.getBody().correlationId());
    }

    @Test
    void handleResourceNotFound_de_negocio_retorna_404_con_mensaje_catalogado() {
        CorrelationIdContext.set(java.util.UUID.fromString(CORRELATION_ID));

        final ResponseEntity<ApiErrorResponse> response = handler.handleApplication(
                new ResourceNotFoundException(EstudianteErrorCode.ERR_ESTUDIANTE_NO_EXISTE, "Detalle tecnico interno."),
                request("/api/v1/estudiantes/13641bab-e3cd-485c-b275-47e7b731e18c")
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("ERR_ESTUDIANTE_NO_EXISTE", response.getBody().code());
        assertEquals("El estudiante consultado no existe.", response.getBody().message());
        assertEquals(CORRELATION_ID, response.getBody().correlationId());
        assertFalse(response.getBody().message().contains("Detalle tecnico"));
    }

    @Test
    void handleDatabaseOperation_retorna_internalServerError_con_mensaje_seguro() {
        final ResponseEntity<ApiErrorResponse> response = handler.handleTechnical(
                new DatabaseOperationException("Stored Procedure fallo por SQLException password", new RuntimeException("sql")),
                request()
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("DATABASE_OPERATION_ERROR", response.getBody().code());
        assertEquals(SAFE_INTERNAL_MESSAGE, response.getBody().message());
        assertDoesNotContainSensitiveDetail(response.getBody().message());
    }

    @Test
    void handleInternalApplication_retorna_500_sin_mensaje_tecnico() {
        final ResponseEntity<ApiErrorResponse> response = handler.handleApplication(
                new InternalApplicationException(CommonErrorCode.INTERNAL_APPLICATION_ERROR, "mensajeTecnicoResultado SQLException password stackTrace"),
                request()
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("INTERNAL_APPLICATION_ERROR", response.getBody().code());
        assertEquals(SAFE_INTERNAL_MESSAGE, response.getBody().message());
        assertDoesNotContainSensitiveDetail(response.getBody().message());
    }

    @Test
    void illegalArgumentException_inesperada_cae_en_handler_generico_500() {
        CorrelationIdContext.set(java.util.UUID.fromString(CORRELATION_ID));

        final ResponseEntity<ApiErrorResponse> response = handler.handleUnexpected(
                new IllegalArgumentException("Dato invalido inesperado"),
                request()
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("INTERNAL_ERROR", response.getBody().code());
        assertEquals(SAFE_INTERNAL_MESSAGE, response.getBody().message());
        assertEquals(CORRELATION_ID, response.getBody().correlationId());
    }

    @Test
    void handleNoResourceFound_retorna_notFound_con_codigo_seguro_y_correlationId() {
        CorrelationIdContext.set(java.util.UUID.fromString(CORRELATION_ID));
        final MockHttpServletRequest request = request("/ruta-que-no-existe");

        final ResponseEntity<ApiErrorResponse> response = handler.handleNoResourceFound(
                new NoResourceFoundException(HttpMethod.GET, "ruta-que-no-existe", "/ruta-que-no-existe"),
                request
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().status());
        assertEquals("Not Found", response.getBody().error());
        assertEquals("RESOURCE_NOT_FOUND", response.getBody().code());
        assertEquals("El recurso solicitado no existe.", response.getBody().message());
        assertEquals("/ruta-que-no-existe", response.getBody().path());
        assertEquals(CORRELATION_ID, response.getBody().correlationId());
        assertFalse(response.getBody().message().contains("No static resource"));
        assertFalse(response.getBody().message().contains("NoResourceFoundException"));
    }

    @Test
    void handleUnexpected_no_expone_detalles_sensibles() {
        final ResponseEntity<ApiErrorResponse> response = handler.handleUnexpected(
                new RuntimeException("SQLException Stored Procedure password token stackTrace RuntimeException"),
                request()
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("INTERNAL_ERROR", response.getBody().code());
        assertEquals(SAFE_INTERNAL_MESSAGE, response.getBody().message());
        assertDoesNotContainSensitiveDetail(response.getBody().message());
        assertFalse(response.getBody().message().contains("RuntimeException"));
    }

    @Test
    void handleFrameworkBadRequest_retorna_respuesta_estandar() throws Exception {
        CorrelationIdContext.set(java.util.UUID.fromString(CORRELATION_ID));

        final ResponseEntity<ApiErrorResponse> response = handler.handleFrameworkBadRequest(
                new org.springframework.web.bind.MissingServletRequestParameterException("page", "Integer"),
                request()
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("INVALID_REQUEST", response.getBody().code());
        assertEquals("No fue posible interpretar la solicitud. Revise el formato y el tipo de los campos enviados.", response.getBody().message());
        assertEquals(CORRELATION_ID, response.getBody().correlationId());
    }

    @Test
    void jsonMalFormado_retorna_400_con_mensaje_publico_seguro() {
        CorrelationIdContext.set(java.util.UUID.fromString(CORRELATION_ID));

        final ResponseEntity<ApiErrorResponse> response = handler.handleFrameworkBadRequest(
                new org.springframework.http.converter.HttpMessageNotReadableException(
                        "JSON parse error: SQLException password token",
                        null,
                        null
                ),
                request()
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("INVALID_REQUEST", response.getBody().code());
        assertEquals("No fue posible interpretar la solicitud. Revise el formato y el tipo de los campos enviados.", response.getBody().message());
        assertEquals(CORRELATION_ID, response.getBody().correlationId());
        assertDoesNotContainSensitiveDetail(response.getBody().message());
    }

    private MockHttpServletRequest request() {
        return request("/api/prueba");
    }

    private MockHttpServletRequest request(final String uri) {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        return request;
    }

    private void assertDoesNotContainSensitiveDetail(final String message) {
        assertFalse(message.contains("mensajeTecnicoResultado"));
        assertFalse(message.contains("technicalMessage"));
        assertFalse(message.contains("SQLException"));
        assertFalse(message.contains("DataAccessException"));
        assertFalse(message.contains("stackTrace"));
        assertFalse(message.contains("Stored Procedure"));
        assertFalse(message.contains("connectionString"));
        assertFalse(message.contains("password"));
        assertFalse(message.contains("token"));
    }
}
