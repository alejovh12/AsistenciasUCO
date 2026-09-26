package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.reporting;

import co.edu.uco.asistenciasuco.application.secondaryports.report.ReporteAsistenciaRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReporteAsistenciaSqlServerAdapterTest {

    private final NamedParameterJdbcOperations jdbc = mock(NamedParameterJdbcOperations.class);
    private final ReporteAsistenciaSqlServerAdapter adapter = new ReporteAsistenciaSqlServerAdapter(jdbc);

    @Test
    @SuppressWarnings("unchecked")
    void consultarReporteAsistenciaGrupo_mapea_fila_completa() throws Exception {
        final UUID grupo = UUID.randomUUID();
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("codigoGrupo")).thenReturn("G1");
        when(rs.getObject("nombreGrupo")).thenReturn("Grupo 1");
        when(rs.getObject("numeroSesion")).thenReturn(3);
        when(rs.getInt("numeroSesion")).thenReturn(3);
        when(rs.getObject("nombreSesion")).thenReturn("Sesion 3");
        when(rs.getObject("fechaHoraInicio")).thenReturn(Timestamp.from(Instant.parse("2026-01-20T08:00:00Z")));
        when(rs.getObject("fechaHoraFin")).thenReturn(Timestamp.from(Instant.parse("2026-01-20T10:00:00Z")));
        when(rs.getObject("documentoEstudiante")).thenReturn("123456789");
        when(rs.getObject("nombreEstudiante")).thenReturn("Ana Perez");
        when(rs.getObject("correoEstudiante")).thenReturn("ana@uco.edu.co");
        when(rs.getObject("asistio")).thenReturn(true);
        when(rs.getBoolean("asistio")).thenReturn(true);
        when(rs.getObject("razonCausa")).thenReturn(null);
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> List.of(((RowMapper<ReporteAsistenciaRow>) invocation.getArgument(2)).mapRow(rs, 0)));

        final List<ReporteAsistenciaRow> resultado = adapter.consultarReporteAsistenciaGrupo(grupo);

        assertEquals(1, resultado.size());
        assertEquals("G1", resultado.getFirst().codigoGrupo());
        assertEquals(3, resultado.getFirst().numeroSesion());
        assertEquals(LocalDateTime.of(2026, 1, 20, 8, 0), resultado.getFirst().fechaHoraInicio());
        assertEquals(LocalDateTime.of(2026, 1, 20, 10, 0), resultado.getFirst().fechaHoraFin());
        assertTrue(resultado.getFirst().asistio());
        assertNull(resultado.getFirst().razonCausa());
        final var params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).query(anyString(), params.capture(), any(RowMapper.class));
        assertEquals(grupo, params.getValue().getValue("idGrupo"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void consultarReporteAsistenciaGrupo_con_numeroSesion_y_asistio_nulos_los_mapea_nulos() throws Exception {
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("codigoGrupo")).thenReturn("G1");
        when(rs.getObject("nombreGrupo")).thenReturn("Grupo 1");
        when(rs.getObject("numeroSesion")).thenReturn(null);
        when(rs.getObject("nombreSesion")).thenReturn("Sesion 3");
        when(rs.getObject("fechaHoraInicio")).thenReturn(null);
        when(rs.getObject("fechaHoraFin")).thenReturn(null);
        when(rs.getObject("documentoEstudiante")).thenReturn("123456789");
        when(rs.getObject("nombreEstudiante")).thenReturn("Ana Perez");
        when(rs.getObject("correoEstudiante")).thenReturn("ana@uco.edu.co");
        when(rs.getObject("asistio")).thenReturn(null);
        when(rs.getObject("razonCausa")).thenReturn("Inasistencia justificada");
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> List.of(((RowMapper<ReporteAsistenciaRow>) invocation.getArgument(2)).mapRow(rs, 0)));

        final List<ReporteAsistenciaRow> resultado = adapter.consultarReporteAsistenciaGrupo(UUID.randomUUID());

        assertNull(resultado.getFirst().numeroSesion());
        assertNull(resultado.getFirst().asistio());
        assertEquals("Inasistencia justificada", resultado.getFirst().razonCausa());
    }

    @Test
    void consultarReporteAsistenciaGrupo_traduce_error_jdbc_a_databaseOperationException() {
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenThrow(new DataAccessResourceFailureException("sin conexion"));

        assertThrows(DatabaseOperationException.class, () -> adapter.consultarReporteAsistenciaGrupo(UUID.randomUUID()));
    }
}
