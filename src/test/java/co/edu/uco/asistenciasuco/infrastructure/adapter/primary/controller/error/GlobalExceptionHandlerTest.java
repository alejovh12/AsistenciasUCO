package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error;

import co.edu.uco.asistenciasuco.application.exception.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.DatabaseOperationException;
import co.edu.uco.asistenciasuco.application.exception.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.exception.ValidationException;
import co.edu.uco.asistenciasuco.infrastructure.correlation.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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

        final ResponseEntity<ApiErrorResponse> response = handler.handleValidation(
                new ValidationException("Dato invalido."),
                request()
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("VALIDATION_ERROR", response.getBody().code());
        assertEquals("Dato invalido.", response.getBody().message());
        assertEquals("/api/prueba", response.getBody().path());
        assertEquals(CORRELATION_ID, response.getBody().correlationId());
    }

    @Test
    void handleConflict_retorna_conflict_sin_exponer_detalles_tecnicos() {
        final ResponseEntity<ApiErrorResponse> response = handler.handleConflict(
                new ConflictException("ERR_UNICIDAD_CORREO", "El correo ya existe."),
                request()
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("ERR_UNICIDAD_CORREO", response.getBody().code());
        assertEquals("El correo ya existe.", response.getBody().message());
    }

    @Test
    void handleDatabaseOperation_retorna_internalServerError_con_mensaje_seguro() {
        final ResponseEntity<ApiErrorResponse> response = handler.handleDatabaseOperation(
                new DatabaseOperationException("Stored Procedure fallo por SQLException password", new RuntimeException("sql")),
                request()
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("DATABASE_OPERATION_ERROR", response.getBody().code());
        assertEquals(SAFE_INTERNAL_MESSAGE, response.getBody().message());
        assertDoesNotContainSensitiveDetail(response.getBody().message());
    }

    @Test
    void handleInternalApplication_con_err_db_unclassified_retorna_500_sin_mensaje_tecnico() {
        final ResponseEntity<ApiErrorResponse> response = handler.handleInternalApplication(
                new InternalApplicationException("ERR_DB_UNCLASSIFIED", "mensajeTecnicoResultado SQLException password stackTrace"),
                request()
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("ERR_DB_UNCLASSIFIED", response.getBody().code());
        assertEquals(SAFE_INTERNAL_MESSAGE, response.getBody().message());
        assertDoesNotContainSensitiveDetail(response.getBody().message());
    }

    @Test
    void illegalArgumentException_inesperada_cae_en_handler_generico_500() {
        final ResponseEntity<ApiErrorResponse> response = handler.handleUnexpected(
                new IllegalArgumentException("Dato invalido inesperado"),
                request()
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("UNEXPECTED_ERROR", response.getBody().code());
        assertEquals(SAFE_INTERNAL_MESSAGE, response.getBody().message());
    }

    @Test
    void handleUnexpected_no_expone_detalles_sensibles() {
        final ResponseEntity<ApiErrorResponse> response = handler.handleUnexpected(
                new RuntimeException("SQLException Stored Procedure password token stackTrace"),
                request()
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(SAFE_INTERNAL_MESSAGE, response.getBody().message());
        assertDoesNotContainSensitiveDetail(response.getBody().message());
    }

    private MockHttpServletRequest request() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/prueba");
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
