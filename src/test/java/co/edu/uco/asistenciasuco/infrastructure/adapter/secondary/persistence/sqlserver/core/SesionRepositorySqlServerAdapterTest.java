package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ActualizarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CerrarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.GenerarSesionesGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.CanonicalStoredProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
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

class SesionRepositorySqlServerAdapterTest {

    private static final UUID CORRELATION = UUID.randomUUID();
    private static final UUID SESSION = UUID.randomUUID();
    private static final UUID GROUP = UUID.randomUUID();
    private static final UUID TEACHER = UUID.randomUUID();
    private final CanonicalStoredProcedureExecutor procedures = mock(CanonicalStoredProcedureExecutor.class);
    private final NamedParameterJdbcOperations jdbc = mock(NamedParameterJdbcOperations.class);
    private final SesionRepositorySqlServerAdapter adapter = new SesionRepositorySqlServerAdapter(procedures, jdbc);

    @AfterEach
    void clearCorrelation() {
        CorrelationIdContext.clear();
    }

    @Test
    void createPassesConfirmedProcedureAndParameters() {
        final LocalDateTime start = LocalDateTime.of(2026, 9, 14, 8, 0);
        CorrelationIdContext.set(CORRELATION);
        adapter.crearSesion(new CrearSesionRepositoryDTO(GROUP, "Tema", "Descripción", start,
                start.plusHours(1), "A101", "PRESENCIAL", TEACHER));

        final var operation = ArgumentCaptor.forClass(String.class);
        final var sql = ArgumentCaptor.forClass(String.class);
        final var params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(procedures).execute(operation.capture(), sql.capture(), params.capture());
        assertEquals("crearSesion", operation.getValue());
        assertTrue(sql.getValue().contains("dbo.usp_crear_sesion"));
        assertEquals(GROUP, params.getValue().getValue("idGrupo"));
        assertEquals(TEACHER, params.getValue().getValue("idDocente"));
        assertEquals(start, params.getValue().getValue("fechaHoraInicio"));
        assertEquals("PRESENCIAL", params.getValue().getValue("tipo"));
        assertEquals(CORRELATION, params.getValue().getValue("idCorrelacion"));
    }

    @Test
    void updateCloseAndGeneratePassThePublicProcedures() {
        final LocalDateTime start = LocalDateTime.of(2026, 9, 14, 8, 0);
        CorrelationIdContext.set(CORRELATION);
        adapter.actualizarSesion(new ActualizarSesionRepositoryDTO(SESSION, "Nuevo", start,
                start.plusHours(2), "B102", "Cambio", TEACHER));
        adapter.cerrarSesion(new CerrarSesionRepositoryDTO(SESSION, TEACHER, "Cierre"));
        adapter.generarSesionesGrupo(new GenerarSesionesGrupoRepositoryDTO(GROUP));

        final var operation = ArgumentCaptor.forClass(String.class);
        final var sql = ArgumentCaptor.forClass(String.class);
        final var params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(procedures, org.mockito.Mockito.times(3)).execute(operation.capture(), sql.capture(), params.capture());
        assertEquals(java.util.List.of("actualizarSesion", "cerrarSesion", "generarSesionesGrupo"), operation.getAllValues());
        assertTrue(sql.getAllValues().get(0).contains("dbo.usp_actualizar_sesion"));
        assertTrue(sql.getAllValues().get(1).contains("dbo.usp_cerrar_sesion"));
        assertTrue(sql.getAllValues().get(2).contains("dbo.usp_generar_sesiones_grupo"));
        assertEquals(SESSION, params.getAllValues().get(0).getValue("idSesion"));
        assertEquals("Nuevo", params.getAllValues().get(0).getValue("nombre"));
        assertEquals(TEACHER, params.getAllValues().get(1).getValue("idDocente"));
        assertEquals(GROUP, params.getAllValues().get(2).getValue("idGrupo"));
        params.getAllValues().forEach(value -> assertEquals(CORRELATION, value.getValue("idCorrelacion")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void queryMapsConfirmedViewColumnsAndReturnsNullWhenAbsent() throws Exception {
        final ResultSet resultSet = mock(ResultSet.class);
        final LocalDateTime start = LocalDateTime.of(2026, 9, 14, 8, 0);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getObject("id")).thenReturn(SESSION.toString());
        when(resultSet.getObject("idGrupo")).thenReturn(GROUP);
        when(resultSet.getObject("nombre")).thenReturn("Sesión");
        when(resultSet.getObject("numero")).thenReturn(1L);
        when(resultSet.getObject("codigo")).thenReturn("S01");
        when(resultSet.getObject("numeroSemana")).thenReturn("2");
        when(resultSet.getObject("codigoGrupo")).thenReturn("G01");
        when(resultSet.getObject("nombreGrupo")).thenReturn("Grupo 1");
        when(resultSet.getObject("fechaHoraInicio")).thenReturn(Timestamp.valueOf(start));
        when(resultSet.getObject("fechaHoraFin")).thenReturn(Timestamp.valueOf(start.plusHours(1)));
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(ResultSetExtractor.class)))
                .thenAnswer(invocation -> ((ResultSetExtractor<SesionRepositoryProjection>) invocation.getArgument(2))
                        .extractData(resultSet));

        final SesionRepositoryProjection result = adapter.consultarSesion(new ConsultarSesionRepositoryDTO(SESSION));
        assertEquals(SESSION, result.getSesion());
        assertEquals(GROUP, result.getGrupo());
        assertEquals("Sesión", result.getNombre());
        assertEquals(1, result.getNumero());
        assertEquals(2, result.getNumeroSemana());
        assertEquals(start, result.getFechaHoraInicio());
        final var sql = ArgumentCaptor.forClass(String.class);
        final var params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).query(sql.capture(), params.capture(), any(ResultSetExtractor.class));
        assertTrue(sql.getValue().contains("FROM dbo.uv_sesion"));
        assertEquals(SESSION, params.getValue().getValue("idSesion"));

        when(resultSet.next()).thenReturn(false);
        assertNull(adapter.consultarSesion(new ConsultarSesionRepositoryDTO(SESSION)));
    }

    @Test
    void rejectsMissingDtoAndWrapsJdbcFailure() {
        assertThrows(CrosscuttingException.class, () -> adapter.crearSesion(null));
        assertThrows(CrosscuttingException.class, () -> adapter.actualizarSesion(null));
        assertThrows(CrosscuttingException.class, () -> adapter.consultarSesion(null));
        assertThrows(CrosscuttingException.class, () -> adapter.cerrarSesion(null));
        assertThrows(CrosscuttingException.class, () -> adapter.generarSesionesGrupo(null));

        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(ResultSetExtractor.class)))
                .thenThrow(new DataAccessResourceFailureException("sin conexión"));
        assertThrows(DatabaseOperationException.class,
                () -> adapter.consultarSesion(new ConsultarSesionRepositoryDTO(SESSION)));
    }
}
