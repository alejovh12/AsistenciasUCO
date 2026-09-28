package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SesionMateriaEstudianteProjection;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SesionMateriaEstudianteSqlServerAdapterTest {

    private final NamedParameterJdbcOperations jdbc = mock(NamedParameterJdbcOperations.class);
    private final SesionMateriaEstudianteSqlServerAdapter adapter = new SesionMateriaEstudianteSqlServerAdapter(jdbc);

    @Test
    @SuppressWarnings("unchecked")
    void consultarSesionesMateria_mapea_proyeccion_completa() throws Exception {
        final UUID estudiante = UUID.randomUUID();
        final UUID asignatura = UUID.randomUUID();
        final UUID id = UUID.randomUUID();
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("id")).thenReturn(id);
        when(rs.getObject("nombre")).thenReturn("Sesion 1");
        when(rs.getObject("numero")).thenReturn(1);
        when(rs.getObject("codigo")).thenReturn("SES-01");
        when(rs.getObject("numeroSemana")).thenReturn(1);
        when(rs.getObject("idGrupo")).thenReturn(UUID.randomUUID());
        when(rs.getObject("codigoGrupo")).thenReturn("G1");
        when(rs.getObject("nombreGrupo")).thenReturn("Grupo 1");
        when(rs.getObject("fechaHoraInicio")).thenReturn(Timestamp.from(Instant.parse("2026-01-20T08:00:00Z")));
        when(rs.getObject("fechaHoraFin")).thenReturn(Timestamp.from(Instant.parse("2026-01-20T10:00:00Z")));
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> List.of(((RowMapper<SesionMateriaEstudianteProjection>) invocation.getArgument(2)).mapRow(rs, 0)));

        final List<SesionMateriaEstudianteProjection> resultado = adapter.consultarSesionesMateria(estudiante, asignatura);

        assertEquals(1, resultado.size());
        assertEquals(id, resultado.getFirst().id());
        assertEquals(LocalDateTime.of(2026, 1, 20, 8, 0), resultado.getFirst().fechaHoraInicio());
        final var params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).query(anyString(), params.capture(), any(RowMapper.class));
        assertEquals(estudiante, params.getValue().getValue("idEstudiante"));
        assertEquals(asignatura, params.getValue().getValue("idAsignatura"));
    }

    @Test
    void consultarSesionesMateria_sin_resultados_retorna_lista_vacia() {
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());

        final List<SesionMateriaEstudianteProjection> resultado = adapter.consultarSesionesMateria(
                UUID.randomUUID(), UUID.randomUUID());

        assertTrue(resultado.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void consultarSesionesMateria_decodifica_datetime2_como_utc_sin_depender_del_timezone_host() throws Exception {
        final TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            final LocalDateTime expectedStart = LocalDateTime.of(2026, 6, 15, 8, 30);
            final LocalDateTime expectedEnd = LocalDateTime.of(2026, 6, 15, 10, 30);
            final Timestamp start = Timestamp.valueOf(expectedStart);
            final Timestamp end = Timestamp.valueOf(expectedEnd);
            final ResultSet rs = mock(ResultSet.class);
            when(rs.getObject("fechaHoraInicio")).thenReturn(start);
            when(rs.getObject("fechaHoraFin")).thenReturn(end);
            when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                    .thenAnswer(invocation -> List.of(((RowMapper<SesionMateriaEstudianteProjection>) invocation.getArgument(2))
                            .mapRow(rs, 0)));

            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
            final SesionMateriaEstudianteProjection projection = adapter.consultarSesionesMateria(
                    UUID.randomUUID(), UUID.randomUUID()).getFirst();

            assertEquals(expectedStart, projection.fechaHoraInicio());
            assertEquals(expectedEnd, projection.fechaHoraFin());
        } finally {
            TimeZone.setDefault(original);
        }
    }
}
