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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class DecanoSqlServerAdapterTest {

    @AfterEach
    void clearCorrelationId() {
        CorrelationIdContext.clear();
    }

    @Test
    void crear_decano_envia_tipo_identificacion_en_segunda_posicion_del_exec() {
        final UUID tipoId = UUID.fromString("22222222-3333-4444-5555-666666666666");
        final UUID correlationId = UUID.fromString("33333333-4444-5555-6666-777777777777");
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
                "nuevo@uco.edu.co", UUID.randomUUID(), "Facultad", "HASH"
        ));

        assertTrue(sql.get().contains("EXEC dbo.usp_crear_decano"));
        assertTrue(sql.get().indexOf("@idDecano") < sql.get().indexOf("@idTipoIdIdentificacion"));
        assertTrue(sql.get().indexOf("@idTipoIdIdentificacion") < sql.get().indexOf("@numeroIdentificacion"));
        assertEquals(tipoId, params.get().getValue("idTipoIdIdentificacion"));
        assertEquals("HASH", params.get().getValue("password"));
        assertEquals(correlationId, params.get().getValue("idCorrelacion"));
    }
}
