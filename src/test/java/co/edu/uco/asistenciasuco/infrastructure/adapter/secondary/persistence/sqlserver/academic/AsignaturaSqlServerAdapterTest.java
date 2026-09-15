package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsignaturaProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.CanonicalStoredProcedureExecutor;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AsignaturaSqlServerAdapterTest {

    private static final UUID CORRELACION = UUID.randomUUID();
    private final NamedParameterJdbcOperations jdbc = mock(NamedParameterJdbcOperations.class);
    private final CanonicalStoredProcedureExecutor procedures = mock(CanonicalStoredProcedureExecutor.class);
    private final AsignaturaSqlServerAdapter adapter = new AsignaturaSqlServerAdapter(jdbc, procedures);

    @AfterEach
    void clearCorrelation() {
        CorrelationIdContext.clear();
    }

    @Test
    void crearAsignatura_ejecuta_procedimiento_esperado() {
        CorrelationIdContext.set(CORRELACION);
        final UUID id = UUID.randomUUID();
        final UUID plan = UUID.randomUUID();

        adapter.crearAsignatura(id, "COD1", "Nombre", 3, plan, 1, "Area", "Componente");

        final var sql = ArgumentCaptor.forClass(String.class);
        final var params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(procedures).execute(org.mockito.ArgumentMatchers.eq("crearAsignatura"), sql.capture(), params.capture());
        assertTrue(sql.getValue().contains("usp_crear_asignatura"));
        assertEquals(id, params.getValue().getValue("idAsignatura"));
        assertEquals("COD1", params.getValue().getValue("codigo"));
        assertEquals(CORRELACION, params.getValue().getValue("idCorrelacion"));
    }

    @Test
    void actualizarAsignatura_ejecuta_procedimiento_esperado() {
        CorrelationIdContext.set(CORRELACION);
        final UUID id = UUID.randomUUID();

        adapter.actualizarAsignatura(id, "COD2", "Nombre2", 4, UUID.randomUUID(), 2, "Area2", "Comp2");

        final var sql = ArgumentCaptor.forClass(String.class);
        verify(procedures).execute(org.mockito.ArgumentMatchers.eq("actualizarAsignatura"), sql.capture(), any(MapSqlParameterSource.class));
        assertTrue(sql.getValue().contains("usp_actualizar_asignatura"));
    }

    @Test
    void toggleEstadoAsignatura_ejecuta_procedimiento_esperado() {
        CorrelationIdContext.set(CORRELACION);
        final UUID id = UUID.randomUUID();

        adapter.toggleEstadoAsignatura(id);

        final var sql = ArgumentCaptor.forClass(String.class);
        final var params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(procedures).execute(org.mockito.ArgumentMatchers.eq("toggleEstadoAsignatura"), sql.capture(), params.capture());
        assertTrue(sql.getValue().contains("usp_toggle_estado_asignatura"));
        assertEquals(id, params.getValue().getValue("idAsignatura"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void consultarAsignaturasPorPrograma_mapea_proyeccion_completa() throws Exception {
        final UUID programa = UUID.randomUUID();
        final UUID id = UUID.randomUUID();
        final ResultSet rs = resultSetConDatosCompletos(id);
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> List.of(((RowMapper<AsignaturaProjection>) invocation.getArgument(2)).mapRow(rs, 0)));

        final List<AsignaturaProjection> resultado = adapter.consultarAsignaturasPorPrograma(programa);

        assertEquals(1, resultado.size());
        assertEquals(id, resultado.getFirst().id());
        assertEquals("COD1", resultado.getFirst().codigo());
        final var sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sql.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));
        assertTrue(sql.getValue().contains("idPrograma = :idPrograma"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void consultarAsignaturasPorPlan_mapea_proyeccion_completa() throws Exception {
        final UUID plan = UUID.randomUUID();
        final UUID id = UUID.randomUUID();
        final ResultSet rs = resultSetConDatosCompletos(id);
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> List.of(((RowMapper<AsignaturaProjection>) invocation.getArgument(2)).mapRow(rs, 0)));

        final List<AsignaturaProjection> resultado = adapter.consultarAsignaturasPorPlan(plan);

        assertEquals(1, resultado.size());
        assertTrue(resultado.getFirst().estaActivaAsignatura());
        final var sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sql.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));
        assertTrue(sql.getValue().contains("idPlanEstudio = :idPlanEstudio"));
    }

    private ResultSet resultSetConDatosCompletos(final UUID id) throws Exception {
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("id")).thenReturn(id);
        when(rs.getObject("codigo")).thenReturn("COD1");
        when(rs.getObject("nombre")).thenReturn("Nombre asignatura");
        when(rs.getObject("credito")).thenReturn(3);
        when(rs.getObject("idArea")).thenReturn(UUID.randomUUID());
        when(rs.getObject("nombreArea")).thenReturn("Area");
        when(rs.getObject("idComponente")).thenReturn(UUID.randomUUID());
        when(rs.getObject("nombreComponente")).thenReturn("Componente");
        when(rs.getObject("idSemestrePlanEstudio")).thenReturn(UUID.randomUUID());
        when(rs.getObject("idPlanEstudio")).thenReturn(UUID.randomUUID());
        when(rs.getObject("idPrograma")).thenReturn(UUID.randomUUID());
        when(rs.getObject("nombrePrograma")).thenReturn("Programa");
        when(rs.getObject("codigoSemestre")).thenReturn("S1");
        when(rs.getObject("estaActivaAsignatura")).thenReturn(true);
        when(rs.getObject("estaActivaTextoAsignatura")).thenReturn("Activa");
        return rs;
    }
}
