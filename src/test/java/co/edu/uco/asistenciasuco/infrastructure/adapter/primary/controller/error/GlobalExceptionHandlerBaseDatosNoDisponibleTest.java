package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error;

import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import org.hibernate.exception.JDBCConnectionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.CannotCreateTransactionException;

import java.net.ConnectException;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientConnectionException;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * REA-02 (revision final P3): con SQL Server sin conexion la API responde 503 {@code ERR_BD_NO_DISPONIBLE} (con
 * {@code Retry-After}), no 500. Un error ordinario de SQL (restriccion de integridad, rechazo de negocio) sigue como antes.
 */
class GlobalExceptionHandlerBaseDatosNoDisponibleTest {

    private static final String CODIGO = "ERR_BD_NO_DISPONIBLE";
    private static final String RESPALDO = "El servicio no está disponible en este momento. Intenta de nuevo en unos minutos.";

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(codigo -> Optional.empty());

    private static MockHttpServletRequest request() {
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/grupos");
        request.setRequestURI("/api/v1/grupos");
        return request;
    }

    static Stream<Exception> caidasDeConexion() {
        return Stream.of(
                new CannotGetJdbcConnectionException("No se pudo obtener la conexion", new SQLException("down")),
                new DataAccessResourceFailureException("recurso no disponible"),
                new JDBCConnectionException("no se pudo abrir la conexion", new SQLException("down")),
                new SQLTransientConnectionException("HikariPool-1 - Connection is not available, request timed out after 30000ms."),
                new SQLNonTransientConnectionException("conexion cerrada"),
                new SQLRecoverableException("enlace caido"),
                new SQLException("The TCP/IP connection to the host failed.", "08S01"),
                new CannotCreateTransactionException("Could not open JPA EntityManager for transaction",
                        new JDBCConnectionException("Unable to acquire JDBC Connection", new SQLException("Connection refused", new ConnectException("refused"))))
        );
    }

    @ParameterizedTest
    @MethodSource("caidasDeConexion")
    void una_excepcion_de_conexion_directa_responde_503(final Exception causa) {
        esperar503(handler.handleUnexpected(causa, request()));
    }

    @ParameterizedTest
    @MethodSource("caidasDeConexion")
    void una_excepcion_de_conexion_envuelta_responde_503(final Exception causa) {
        esperar503(handler.handleUnexpected(new RuntimeException("falla", causa), request()));
    }

    @Test
    void el_fallo_tecnico_de_los_adaptadores_con_causa_de_conexion_responde_503() {
        final DatabaseOperationException tecnico = new DatabaseOperationException("No fue posible ejecutar el procedimiento.",
                new CannotGetJdbcConnectionException("sin conexion", new SQLException("down")));

        esperar503(handler.handleTechnical(tecnico, request()));
    }

    @Test
    void la_excepcion_de_aplicacion_que_envuelve_un_fallo_de_conexion_responde_503() {
        final InternalApplicationException envuelta = new InternalApplicationException("No fue posible verificar la autorizacion.",
                new DatabaseOperationException("consulta", new SQLTransientConnectionException("pool agotado")));

        esperar503(handler.handleApplication(envuelta, request()));
    }

    @Test
    void el_texto_del_catalogo_se_usa_si_esta_disponible() {
        final GlobalExceptionHandler conCatalogo = new GlobalExceptionHandler(
                codigo -> CODIGO.equals(codigo) ? Optional.of("Texto institucional del catalogo.") : Optional.empty());

        final ResponseEntity<ApiErrorResponse> respuesta = conCatalogo.handleUnexpected(
                new CannotGetJdbcConnectionException("sin conexion", new SQLException("down")), request());

        assertEquals("Texto institucional del catalogo.", respuesta.getBody().message());
    }

    @Test
    void si_el_catalogo_tambien_cae_con_la_bd_se_usa_el_respaldo_del_enum() {
        final GlobalExceptionHandler catalogoCaido = new GlobalExceptionHandler(codigo -> {
            throw new CannotGetJdbcConnectionException("el catalogo vive en la misma BD", new SQLException("down"));
        });

        esperar503(catalogoCaido.handleUnexpected(
                new CannotGetJdbcConnectionException("sin conexion", new SQLException("down")), request()));
    }

    @Test
    void una_causa_ciclica_no_cuelga_el_recorrido() {
        final RuntimeException a = new RuntimeException("a");
        final RuntimeException b = new RuntimeException("b", a);
        a.initCause(b);

        final ResponseEntity<ApiErrorResponse> respuesta = handler.handleUnexpected(a, request());

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, respuesta.getStatusCode());
    }

    @Test
    void una_violacion_de_integridad_sigue_siendo_500_generico() {
        final ResponseEntity<ApiErrorResponse> respuesta = handler.handleUnexpected(
                new DataIntegrityViolationException("Violation of UNIQUE KEY constraint", new SQLException("dup", "23000", 2627)),
                request());

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, respuesta.getStatusCode());
        assertEquals("INTERNAL_ERROR", respuesta.getBody().code());
        assertFalse(respuesta.getHeaders().containsHeader(HttpHeaders.RETRY_AFTER));
    }

    @Test
    void un_fallo_tecnico_de_sql_que_no_es_de_conexion_sigue_igual() {
        final ResponseEntity<ApiErrorResponse> respuesta = handler.handleTechnical(
                new DatabaseOperationException("deadlock", new SQLException("deadlock victim", "40001", 1205)), request());

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, respuesta.getStatusCode());
        assertEquals("DATABASE_OPERATION_ERROR", respuesta.getBody().code());
    }

    @Test
    void una_excepcion_de_aplicacion_con_causa_ajena_a_la_bd_sigue_en_500() {
        final ResponseEntity<ApiErrorResponse> respuesta = handler.handleApplication(
                new InternalApplicationException("integridad", new IllegalStateException("x")), request());

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, respuesta.getStatusCode());
    }

    @Test
    void un_cannot_create_transaction_sin_causa_de_conexion_no_cuenta_como_caida() {
        final ResponseEntity<ApiErrorResponse> respuesta = handler.handleUnexpected(
                new CannotCreateTransactionException("otra razon", new IllegalStateException("x")), request());

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, respuesta.getStatusCode());
    }

    private static void esperar503(final ResponseEntity<ApiErrorResponse> respuesta) {
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, respuesta.getStatusCode());
        assertEquals(CODIGO, respuesta.getBody().code());
        assertEquals(503, respuesta.getBody().status());
        assertEquals(RESPALDO, respuesta.getBody().message());
        assertEquals("/api/v1/grupos", respuesta.getBody().path());
        assertEquals(GlobalExceptionHandler.RETRY_AFTER_SEGUNDOS, respuesta.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
        assertNotNull(respuesta.getBody().timestamp());
        assertTrue(respuesta.getHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
