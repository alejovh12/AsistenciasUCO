package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.HorarioDocenteProjection;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HorarioDocenteSqlServerAdapterTest {

    private final NamedParameterJdbcOperations jdbc = mock(NamedParameterJdbcOperations.class);
    private final HorarioDocenteSqlServerAdapter adapter = new HorarioDocenteSqlServerAdapter(jdbc);

    @Test
    @SuppressWarnings("unchecked")
    void consultarHorarioDocente_mapea_proyeccion_completa() throws Exception {
        final UUID docente = UUID.randomUUID();
        final UUID id = UUID.randomUUID();
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("id")).thenReturn(id);
        when(rs.getObject("idDocente")).thenReturn(docente);
        when(rs.getObject("idGrupo")).thenReturn(UUID.randomUUID());
        when(rs.getObject("codigoMateria")).thenReturn("MAT-01");
        when(rs.getObject("nombreMateria")).thenReturn("Calculo");
        when(rs.getObject("seccion")).thenReturn("G1");
        when(rs.getObject("dia")).thenReturn("LUNES");
        when(rs.getObject("horaInicio")).thenReturn(Time.valueOf("08:00:00"));
        when(rs.getObject("horaFin")).thenReturn(Time.valueOf("10:00:00"));
        when(rs.getObject("aula")).thenReturn("Aula 1");
        when(rs.getObject("totalEstudiantes")).thenReturn(25);
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> List.of(((RowMapper<HorarioDocenteProjection>) invocation.getArgument(2)).mapRow(rs, 0)));

        final List<HorarioDocenteProjection> resultado = adapter.consultarHorarioDocente(docente);

        assertEquals(1, resultado.size());
        assertEquals(id, resultado.getFirst().id());
        assertEquals(LocalTime.of(8, 0), resultado.getFirst().horaInicio());
        assertEquals(25, resultado.getFirst().totalEstudiantes());
        final var params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).query(anyString(), params.capture(), any(RowMapper.class));
        assertEquals(docente, params.getValue().getValue("idDocente"));
    }

    @Test
    void consultarHorarioDocente_sin_resultados_retorna_lista_vacia() {
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());

        final List<HorarioDocenteProjection> resultado = adapter.consultarHorarioDocente(UUID.randomUUID());

        assertTrue(resultado.isEmpty());
    }
}
