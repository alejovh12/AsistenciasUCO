package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.academic;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.CanonicalStoredProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CierrePeriodoSqlServerAdapterTest {

    private static final UUID CORRELACION = UUID.randomUUID();
    private final CanonicalStoredProcedureExecutor procedures = mock(CanonicalStoredProcedureExecutor.class);
    private final CierrePeriodoSqlServerAdapter adapter = new CierrePeriodoSqlServerAdapter(procedures);

    @AfterEach
    void clearCorrelation() {
        CorrelationIdContext.clear();
    }

    @Test
    void ejecutarCierreMasivoPeriodo_envia_idActor_y_idUsuarioEjecutor() {
        CorrelationIdContext.set(CORRELACION);
        final UUID actor = UUID.randomUUID();

        adapter.ejecutarCierreMasivoPeriodo("2026-1", actor.toString(), actor);

        final var sql = ArgumentCaptor.forClass(String.class);
        final var params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(procedures).execute(eq("ejecutarCierreMasivoPeriodo"), sql.capture(), params.capture());
        assertTrue(sql.getValue().contains("usp_ejecutar_cierre_masivo_periodo"));
        assertTrue(sql.getValue().contains("@idUsuarioEjecutor"));
        assertEquals("2026-1", params.getValue().getValue("codigoPeriodo"));
        assertEquals(actor.toString(), params.getValue().getValue("idActor"));
        assertEquals(actor, params.getValue().getValue("idUsuarioEjecutor"));
        assertEquals(CORRELACION, params.getValue().getValue("idCorrelacion"));
    }
}
