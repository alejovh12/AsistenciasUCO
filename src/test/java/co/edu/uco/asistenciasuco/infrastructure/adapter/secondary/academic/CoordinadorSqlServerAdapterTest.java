package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CoordinadorProjection;
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

class CoordinadorSqlServerAdapterTest {

    private static final UUID CORRELACION = UUID.randomUUID();
    private final NamedParameterJdbcOperations jdbc = mock(NamedParameterJdbcOperations.class);
    private final CanonicalStoredProcedureExecutor procedures = mock(CanonicalStoredProcedureExecutor.class);
    private final CoordinadorSqlServerAdapter adapter = new CoordinadorSqlServerAdapter(jdbc, procedures);

    @AfterEach
    void clearCorrelation() {
        CorrelationIdContext.clear();
    }

    @Test
    void crearCoordinador_ejecuta_procedimiento_con_parametros_correctos() {
        CorrelationIdContext.set(CORRELACION);
        final UUID id = UUID.randomUUID();
        final UUID programa = UUID.randomUUID();
        final UUID facultad = UUID.randomUUID();

        adapter.crearCoordinador(id, "123456", "ANA", "MARIA", "PEREZ", "GOMEZ",
                "ana@uco.edu.co", programa, facultad, "HASH");

        final var sql = ArgumentCaptor.forClass(String.class);
        final var params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(procedures).execute(eq("crearCoordinador"), sql.capture(), params.capture());
        assertTrue(sql.getValue().contains("usp_crear_coordinador"));
        assertEquals(id, params.getValue().getValue("idCoordinador"));
        assertEquals(programa, params.getValue().getValue("idPrograma"));
        assertEquals(facultad, params.getValue().getValue("idFacultad"));
        assertEquals("HASH", params.getValue().getValue("password"));
        assertEquals(CORRELACION, params.getValue().getValue("idCorrelacion"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void consultarCoordinadoresPorFacultad_mapea_proyeccion() throws Exception {
        final UUID facultad = UUID.randomUUID();
        final UUID id = UUID.randomUUID();
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("id")).thenReturn(id);
        when(rs.getObject("idUsuario")).thenReturn(UUID.randomUUID());
        when(rs.getObject("numeroIdentificacion")).thenReturn("123456");
        when(rs.getObject("nombreCompleto")).thenReturn("Ana Perez");
        when(rs.getObject("idPrograma")).thenReturn(UUID.randomUUID());
        when(rs.getObject("nombrePrograma")).thenReturn("Ingenieria de Sistemas");
        when(rs.getObject("estaActivoCoordinador")).thenReturn(true);
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> List.of(((RowMapper<CoordinadorProjection>) invocation.getArgument(2)).mapRow(rs, 0)));

        final List<CoordinadorProjection> resultado = adapter.consultarCoordinadoresPorFacultad(facultad);

        assertEquals(1, resultado.size());
        assertEquals(id, resultado.getFirst().id());
        assertTrue(resultado.getFirst().estaActivoCoordinador());
        final var params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).query(anyString(), params.capture(), any(RowMapper.class));
        assertEquals(facultad, params.getValue().getValue("idFacultad"));
    }
}
