package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoCommandPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.CanonicalProcedureResult;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.CanonicalStoredProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class DecanoSqlServerAdapterTest {

    @AfterEach
    void clearCorrelationId() {
        CorrelationIdContext.clear();
    }

    @Test
    void crear_decano_no_envia_tipo_identificacion_y_propaga_usuario_ejecutor() {
        final UUID tipoId = UUID.fromString("22222222-3333-4444-5555-666666666666");
        final UUID correlationId = UUID.fromString("33333333-4444-5555-6666-777777777777");
        final UUID usuarioEjecutor = UUID.fromString("44444444-5555-6666-7777-888888888888");
        final AtomicReference<String> sql = new AtomicReference<>();
        final AtomicReference<MapSqlParameterSource> params = new AtomicReference<>();
        final NamedParameterJdbcOperations jdbc = mock(NamedParameterJdbcOperations.class);
        final CanonicalStoredProcedureExecutor executor = new CanonicalStoredProcedureExecutor(jdbc) {
            @Override
            public CanonicalProcedureResult execute(
                    final String operation, final String statement, final MapSqlParameterSource parameters
            ) {
                assertEquals("crearDecano", operation);
                sql.set(statement);
                params.set(parameters);
                return new CanonicalProcedureResult(correlationId, "ok", "", true);
            }
        };
        CorrelationIdContext.set(correlationId);
        final var adapter = new DecanoSqlServerAdapter(jdbc, executor);
        adapter.crearDecano(new DecanoCommandPort.CrearDecanoCommand(
                UUID.randomUUID(), tipoId, 123456789, "ANA", "MARIA", "PEREZ", "GOMEZ",
                "nuevo@uco.edu.co", UUID.randomUUID(), "Facultad", "HASH", usuarioEjecutor
        ));

        assertTrue(sql.get().contains("EXEC dbo.usp_crear_decano"));
        assertTrue(sql.get().contains("@idUsuarioEjecutor"));
        assertFalse(sql.get().contains("@idTipoIdIdentificacion"));
        assertFalse(params.get().hasValue("idTipoIdIdentificacion"));
        assertEquals(usuarioEjecutor, params.get().getValue("idUsuarioEjecutor"));
        assertEquals("HASH", params.get().getValue("password"));
        assertEquals(correlationId, params.get().getValue("idCorrelacion"));
    }
}
