package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CanonicalStoredProcedureExecutorTest {

    private static final UUID CORRELATION = UUID.randomUUID();
    private final NamedParameterJdbcOperations jdbc = mock(NamedParameterJdbcOperations.class);
    private final CanonicalStoredProcedureExecutor executor = new CanonicalStoredProcedureExecutor(jdbc);

    @AfterEach
    void clearCorrelation() {
        CorrelationIdContext.clear();
    }

    @Test
    @SuppressWarnings("unchecked")
    void execute_con_resultado_exitoso_y_correlacion_valida_retorna_resultado() {
        CorrelationIdContext.set(CORRELATION);
        final CanonicalProcedureResult expected = new CanonicalProcedureResult(CORRELATION, "ok", "detalle", true);
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(expected));

        final CanonicalProcedureResult result = executor.execute("operacion", "EXEC sp", new MapSqlParameterSource());

        assertEquals(expected, result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void execute_con_operacion_nula_normaliza_a_unknown_y_funciona() {
        CorrelationIdContext.set(CORRELATION);
        final CanonicalProcedureResult expected = new CanonicalProcedureResult(CORRELATION, "ok", "detalle", true);
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(expected));

        final CanonicalProcedureResult result = executor.execute(null, "EXEC sp", new MapSqlParameterSource());

        assertEquals(expected, result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void execute_con_numero_de_resultados_distinto_de_uno_lanza_excepcion() {
        CorrelationIdContext.set(CORRELATION);
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());

        assertThrows(DatabaseOperationException.class,
                () -> executor.execute("operacion", "EXEC sp", new MapSqlParameterSource()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void execute_con_correlacion_no_coincidente_lanza_excepcion() {
        CorrelationIdContext.set(CORRELATION);
        final CanonicalProcedureResult mismatched = new CanonicalProcedureResult(UUID.randomUUID(), "ok", "detalle", true);
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(mismatched));

        assertThrows(DatabaseOperationException.class,
                () -> executor.execute("operacion", "EXEC sp", new MapSqlParameterSource()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void execute_con_resultado_fallido_traduce_el_error_de_negocio() {
        CorrelationIdContext.set(CORRELATION);
        final CanonicalProcedureResult failed = new CanonicalProcedureResult(
                CORRELATION, "La operacion no pudo completarse por una regla nueva.", "detalle tecnico", false);
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(failed));

        assertThrows(DatabaseOperationException.class,
                () -> executor.execute("operacion", "EXEC sp", new MapSqlParameterSource()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void execute_traduce_error_jdbc_a_databaseOperationException() {
        CorrelationIdContext.set(CORRELATION);
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenThrow(new DataAccessResourceFailureException("sin conexion"));

        assertThrows(DatabaseOperationException.class,
                () -> executor.execute("operacion", "EXEC sp", new MapSqlParameterSource()));
    }
}
