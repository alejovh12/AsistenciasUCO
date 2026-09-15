package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.FacultadProjection;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.sql.ResultSet;
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

class FacultadSqlServerAdapterTest {

    private final NamedParameterJdbcOperations jdbc = mock(NamedParameterJdbcOperations.class);
    private final FacultadSqlServerAdapter adapter = new FacultadSqlServerAdapter(jdbc);

    @Test
    @SuppressWarnings("unchecked")
    void consultarFacultades_mapea_todas_las_columnas() throws Exception {
        final UUID id = UUID.randomUUID();
        final ResultSet rs = resultSetCompleto(id);
        when(jdbc.query(anyString(), any(RowMapper.class)))
                .thenAnswer(invocation -> List.of(((RowMapper<FacultadProjection>) invocation.getArgument(1)).mapRow(rs, 0)));

        final List<FacultadProjection> resultado = adapter.consultarFacultades();

        assertEquals(1, resultado.size());
        assertEquals(id, resultado.getFirst().id());
        assertEquals("Ingenieria", resultado.getFirst().nombreFacultad());
        assertTrue(resultado.getFirst().estaActivaFacultad());
        final var sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sql.capture(), any(RowMapper.class));
        assertTrue(sql.getValue().contains("FROM dbo.uv_facultad"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void consultarFacultadPorId_retorna_presente_cuando_existe() throws Exception {
        final UUID id = UUID.randomUUID();
        final ResultSet rs = resultSetCompleto(id);
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> List.of(((RowMapper<FacultadProjection>) invocation.getArgument(2)).mapRow(rs, 0)));

        final Optional<FacultadProjection> resultado = adapter.consultarFacultadPorId(id);

        assertTrue(resultado.isPresent());
        assertEquals(id, resultado.get().id());
        final var params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).query(anyString(), params.capture(), any(RowMapper.class));
        assertEquals(id, params.getValue().getValue("idFacultad"));
    }

    @Test
    void consultarFacultadPorId_retorna_vacio_cuando_no_existe() {
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());

        final Optional<FacultadProjection> resultado = adapter.consultarFacultadPorId(UUID.randomUUID());

        assertTrue(resultado.isEmpty());
    }

    private ResultSet resultSetCompleto(final UUID id) throws Exception {
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("id")).thenReturn(id);
        when(rs.getObject("nombreFacultad")).thenReturn("Ingenieria");
        when(rs.getObject("idInstitucion")).thenReturn(UUID.randomUUID());
        when(rs.getObject("nombreInstitucion")).thenReturn("UCO");
        when(rs.getObject("idDecano")).thenReturn(UUID.randomUUID());
        when(rs.getObject("nombreCompletoDecano")).thenReturn("Decano Uno");
        when(rs.getObject("estaActivaFacultad")).thenReturn(true);
        when(rs.getObject("estaActivaTextoFacultad")).thenReturn("Activa");
        return rs;
    }
}
