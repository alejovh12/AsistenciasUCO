package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PeriodoAcademicoProjection;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.sql.Date;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PeriodoAcademicoSqlServerAdapterTest {

    private final NamedParameterJdbcOperations jdbc = mock(NamedParameterJdbcOperations.class);
    private final PeriodoAcademicoSqlServerAdapter adapter = new PeriodoAcademicoSqlServerAdapter(jdbc);

    @Test
    @SuppressWarnings("unchecked")
    void consultarPeriodosAcademicos_mapea_fechas_y_anio() throws Exception {
        final UUID id = UUID.randomUUID();
        final ResultSet rs = resultSetCompleto(id);
        when(jdbc.query(anyString(), any(RowMapper.class)))
                .thenAnswer(invocation -> List.of(((RowMapper<PeriodoAcademicoProjection>) invocation.getArgument(1)).mapRow(rs, 0)));

        final List<PeriodoAcademicoProjection> resultado = adapter.consultarPeriodosAcademicos();

        assertEquals(1, resultado.size());
        assertEquals(id, resultado.getFirst().id());
        assertEquals(LocalDate.of(2026, 1, 20), resultado.getFirst().fechaInicio());
        assertEquals(2026, resultado.getFirst().anio());
        final var sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sql.capture(), any(RowMapper.class));
        assertTrue(sql.getValue().contains("FROM dbo.uv_periodo_academico"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void consultarPeriodoAcademicoPorId_retorna_presente_cuando_existe() throws Exception {
        final UUID id = UUID.randomUUID();
        final ResultSet rs = resultSetCompleto(id);
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> List.of(((RowMapper<PeriodoAcademicoProjection>) invocation.getArgument(2)).mapRow(rs, 0)));

        final Optional<PeriodoAcademicoProjection> resultado = adapter.consultarPeriodoAcademicoPorId(id);

        assertTrue(resultado.isPresent());
        assertEquals(id, resultado.get().id());
        final var params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).query(anyString(), params.capture(), any(RowMapper.class));
        assertEquals(id, params.getValue().getValue("idPeriodoAcademico"));
    }

    @Test
    void consultarPeriodoAcademicoPorId_retorna_vacio_cuando_no_existe() {
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());

        final Optional<PeriodoAcademicoProjection> resultado = adapter.consultarPeriodoAcademicoPorId(UUID.randomUUID());

        assertTrue(resultado.isEmpty());
    }

    private ResultSet resultSetCompleto(final UUID id) throws Exception {
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("id")).thenReturn(id);
        when(rs.getObject("idInstitucion")).thenReturn(UUID.randomUUID());
        when(rs.getObject("nombreInstitucion")).thenReturn("UCO");
        when(rs.getObject("nombre")).thenReturn("2026-1");
        when(rs.getObject("codigo")).thenReturn("2026-1");
        when(rs.getObject("fechaInicio")).thenReturn(Date.valueOf("2026-01-20"));
        when(rs.getObject("fechaFin")).thenReturn(Date.valueOf("2026-05-30"));
        when(rs.getObject("anio")).thenReturn(2026);
        return rs;
    }
}
