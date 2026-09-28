package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.HorarioEstudianteProjection;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.sql.ResultSet;
import java.sql.Time;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HorarioEstudianteSqlServerAdapterTest {

    private final NamedParameterJdbcOperations jdbc = mock(NamedParameterJdbcOperations.class);
    private final HorarioEstudianteSqlServerAdapter adapter = new HorarioEstudianteSqlServerAdapter(jdbc);

    @Test
    @SuppressWarnings("unchecked")
    void consultarHorarioEstudiante_mapea_proyeccion_completa() throws Exception {
        final UUID estudiante = UUID.randomUUID();
        final UUID id = UUID.randomUUID();
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("id")).thenReturn(id);
        when(rs.getObject("idEstudiante")).thenReturn(estudiante);
        when(rs.getObject("idGrupo")).thenReturn(UUID.randomUUID());
        when(rs.getObject("codigoMateria")).thenReturn("MAT-01");
        when(rs.getObject("nombreMateria")).thenReturn("Calculo");
        when(rs.getObject("grupo")).thenReturn("G1");
        when(rs.getObject("dia")).thenReturn("LUNES");
        when(rs.getObject("horaInicio")).thenReturn(Time.valueOf("08:00:00"));
        when(rs.getObject("horaFin")).thenReturn(Time.valueOf("10:00:00"));
        when(rs.getObject("docente")).thenReturn("Docente Uno");
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> List.of(((RowMapper<HorarioEstudianteProjection>) invocation.getArgument(2)).mapRow(rs, 0)));

        final List<HorarioEstudianteProjection> resultado = adapter.consultarHorarioEstudiante(estudiante);

        assertEquals(1, resultado.size());
        assertEquals(id, resultado.getFirst().id());
        assertEquals(LocalTime.of(8, 0), resultado.getFirst().horaInicio());
        assertEquals("Calculo", resultado.getFirst().nombreMateria());
        final var sql = ArgumentCaptor.forClass(String.class);
        final var params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).query(sql.capture(), params.capture(), any(RowMapper.class));
        assertFalse(sql.getValue().toLowerCase(java.util.Locale.ROOT).contains("aula"));
        assertEquals(estudiante, params.getValue().getValue("idEstudiante"));
    }

    @Test
    void consultarHorarioEstudiante_sin_resultados_retorna_lista_vacia() {
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());

        final List<HorarioEstudianteProjection> resultado = adapter.consultarHorarioEstudiante(UUID.randomUUID());

        assertTrue(resultado.isEmpty());
    }
}
