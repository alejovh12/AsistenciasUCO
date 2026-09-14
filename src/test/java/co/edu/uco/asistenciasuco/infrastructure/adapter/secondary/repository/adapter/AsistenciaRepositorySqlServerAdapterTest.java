package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.adapter;

import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsistenciasPorGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciaAutonomaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciasSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistroAsistenciaSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ResolverSolicitudRevisionAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.SolicitarRevisionAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.AsistenciaRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.procedure.CanonicalStoredProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AsistenciaRepositorySqlServerAdapterTest {

    private static final UUID CORRELATION = UUID.randomUUID();
    private static final UUID SESSION = UUID.randomUUID();
    private static final UUID GROUP = UUID.randomUUID();
    private static final UUID STUDENT = UUID.randomUUID();
    private final CanonicalStoredProcedureExecutor procedures = mock(CanonicalStoredProcedureExecutor.class);
    private final NamedParameterJdbcOperations jdbc = mock(NamedParameterJdbcOperations.class);
    private final AsistenciaRepositorySqlServerAdapter adapter = new AsistenciaRepositorySqlServerAdapter(jdbc, procedures);

    @AfterEach
    void clearCorrelation() {
        CorrelationIdContext.clear();
    }

    @Test
    void batchRegistrationSerializesEntriesAndCallsPublicProcedure() {
        CorrelationIdContext.set(CORRELATION);
        adapter.registrarAsistenciasSesion(new RegistrarAsistenciasSesionRepositoryDTO(SESSION,
                List.of(new RegistroAsistenciaSesionRepositoryDTO(STUDENT, "ASISTIO"))));

        final var operation = ArgumentCaptor.forClass(String.class);
        final var sql = ArgumentCaptor.forClass(String.class);
        final var params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(procedures).execute(operation.capture(), sql.capture(), params.capture());
        assertEquals("registrarAsistenciasSesion", operation.getValue());
        assertTrue(sql.getValue().contains("dbo.usp_registrar_asistencias_sesion"));
        assertEquals(SESSION, params.getValue().getValue("idSesion"));
        final String serialized = (String) params.getValue().getValue("asistenciaJSON");
        assertTrue(serialized.contains(STUDENT.toString()));
        assertTrue(serialized.contains("ASISTIO"));
        assertEquals(CORRELATION, params.getValue().getValue("idCorrelacion"));
    }

    @Test
    void autonomousRegistrationAndReviewUseExpectedProceduresAndParameters() {
        final UUID request = UUID.randomUUID();
        final UUID teacher = UUID.randomUUID();
        CorrelationIdContext.set(CORRELATION);
        adapter.registrarAsistenciaAutonoma(new RegistrarAsistenciaAutonomaRepositoryDTO(STUDENT, SESSION, "123456"));
        adapter.solicitarRevisionAsistencia(new SolicitarRevisionAsistenciaRepositoryDTO(STUDENT, SESSION,
                "SALUD", "Justificación", "soporte.pdf", "https://example.com/soporte"));
        adapter.resolverSolicitudRevisionAsistencia(new ResolverSolicitudRevisionAsistenciaRepositoryDTO(request,
                teacher, "APROBAR", "Aceptada"));

        final var operation = ArgumentCaptor.forClass(String.class);
        final var sql = ArgumentCaptor.forClass(String.class);
        final var params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(procedures, org.mockito.Mockito.times(3)).execute(operation.capture(), sql.capture(), params.capture());
        assertEquals(List.of("registrarAsistenciaAutonoma", "solicitarRevisionAsistencia",
                "resolverSolicitudRevisionAsistencia"), operation.getAllValues());
        assertTrue(sql.getAllValues().get(0).contains("dbo.usp_registrar_asistencia_estudiante_autonomo"));
        assertTrue(sql.getAllValues().get(1).contains("dbo.usp_radicar_solicitud_revision_asistencia"));
        assertTrue(sql.getAllValues().get(2).contains("dbo.usp_resolver_solicitud_revision_asistencia"));
        assertEquals("123456", params.getAllValues().get(0).getValue("codigoVerificacion"));
        assertEquals("soporte.pdf", params.getAllValues().get(1).getValue("soporteNombre"));
        assertEquals(request, params.getAllValues().get(2).getValue("idSolicitud"));
        assertEquals(teacher, params.getAllValues().get(2).getValue("idDocente"));
        params.getAllValues().forEach(value -> assertEquals(CORRELATION, value.getValue("idCorrelacion")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void queryMapsAttendanceFromConfirmedView() throws Exception {
        final UUID attendance = UUID.randomUUID();
        final ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getObject("asistencia")).thenReturn(attendance.toString());
        when(resultSet.getObject("estudiante")).thenReturn(STUDENT);
        when(resultSet.getObject("grupo")).thenReturn(GROUP.toString());
        when(resultSet.getObject("sesion")).thenReturn(SESSION);
        when(resultSet.getBoolean("presente")).thenReturn(true);
        when(resultSet.getObject("observacion")).thenReturn("A tiempo");
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> List.of(((RowMapper<AsistenciaRepositoryProjection>) invocation.getArgument(2))
                        .mapRow(resultSet, 0)));

        final List<AsistenciaRepositoryProjection> result = adapter.consultarAsistenciasPorGrupo(
                new ConsultarAsistenciasPorGrupoRepositoryDTO(GROUP, SESSION));
        assertEquals(1, result.size());
        assertEquals(attendance, result.getFirst().getAsistencia());
        assertEquals(STUDENT, result.getFirst().getEstudiante());
        assertTrue(result.getFirst().isPresente());
        assertEquals("A tiempo", result.getFirst().getObservacion());
        final var sql = ArgumentCaptor.forClass(String.class);
        final var params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).query(sql.capture(), params.capture(), any(RowMapper.class));
        assertTrue(sql.getValue().contains("dbo.uv_detalle_asistencia"));
        assertEquals(GROUP, params.getValue().getValue("idGrupo"));
        assertEquals(SESSION, params.getValue().getValue("idSesion"));
    }

    @Test
    void rejectsMissingDtosAndWrapsJdbcFailure() {
        assertThrows(FeatureUnavailableException.class, () -> adapter.registrarAsistencia(null));
        assertThrows(CrosscuttingException.class, () -> adapter.registrarAsistenciasSesion(null));
        assertThrows(CrosscuttingException.class, () -> adapter.registrarAsistenciaAutonoma(null));
        assertThrows(CrosscuttingException.class, () -> adapter.consultarAsistenciasPorGrupo(null));
        assertThrows(CrosscuttingException.class, () -> adapter.solicitarRevisionAsistencia(null));
        assertThrows(CrosscuttingException.class, () -> adapter.resolverSolicitudRevisionAsistencia(null));

        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenThrow(new DataAccessResourceFailureException("sin conexión"));
        assertThrows(DatabaseOperationException.class,
                () -> adapter.consultarAsistenciasPorGrupo(new ConsultarAsistenciasPorGrupoRepositoryDTO(GROUP, null)));
    }
}
