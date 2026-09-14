package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PlanEstudioProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.procedure.CanonicalStoredProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlanEstudioSqlServerAdapterTest {

    private static final UUID CORRELACION = UUID.randomUUID();
    private final NamedParameterJdbcOperations jdbc = mock(NamedParameterJdbcOperations.class);
    private final CanonicalStoredProcedureExecutor procedures = mock(CanonicalStoredProcedureExecutor.class);
    private final PlanEstudioSqlServerAdapter adapter = new PlanEstudioSqlServerAdapter(jdbc, procedures);

    @AfterEach
    void clearCorrelation() {
        CorrelationIdContext.clear();
    }

    @Test
    void registrarOActualizarPlanEstudio_ejecuta_procedimiento_esperado() {
        CorrelationIdContext.set(CORRELACION);
        final UUID id = UUID.randomUUID();
        final UUID programa = UUID.randomUUID();

        adapter.registrarOActualizarPlanEstudio(id, programa, "PE01", "Plan 2026");

        final var sql = ArgumentCaptor.forClass(String.class);
        final var params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(procedures).execute(eq("registrarOActualizarPlanEstudio"), sql.capture(), params.capture());
        assertTrue(sql.getValue().contains("usp_registrar_o_actualizar_plan_estudio"));
        assertEquals(id, params.getValue().getValue("idPlanEstudio"));
        assertEquals("PE01", params.getValue().getValue("codigo"));
        assertEquals(CORRELACION, params.getValue().getValue("idCorrelacion"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void consultarPlanesPorPrograma_mapea_proyeccion() throws Exception {
        final UUID programa = UUID.randomUUID();
        final UUID id = UUID.randomUUID();
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("id")).thenReturn(id);
        when(rs.getObject("idPrograma")).thenReturn(programa);
        when(rs.getObject("nombrePrograma")).thenReturn("Ingenieria de Sistemas");
        when(rs.getObject("inp")).thenReturn("INP-01");
        when(rs.getObject("estaActivoPlanEstudio")).thenReturn(true);
        when(rs.getObject("estaActivoTextoPlanEstudio")).thenReturn("Activo");
        when(rs.getObject("justificacionEstado")).thenReturn(null);
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> List.of(((RowMapper<PlanEstudioProjection>) invocation.getArgument(2)).mapRow(rs, 0)));

        final List<PlanEstudioProjection> resultado = adapter.consultarPlanesPorPrograma(programa);

        assertEquals(1, resultado.size());
        assertEquals(id, resultado.getFirst().id());
        assertTrue(resultado.getFirst().estaActivoPlanEstudio());
        final var params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).query(anyString(), params.capture(), any(RowMapper.class));
        assertEquals(programa, params.getValue().getValue("idPrograma"));
    }
}
