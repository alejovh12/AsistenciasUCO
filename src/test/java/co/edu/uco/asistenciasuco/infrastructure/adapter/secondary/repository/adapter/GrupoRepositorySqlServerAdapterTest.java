package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.adapter;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.features.grupo.exception.GrupoErrorCode;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarEstudianteRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.GrupoRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.RegistrarEstudianteRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.error.DatabaseErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.mapper.GrupoRepositoryRowMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.procedure.CanonicalProcedureResult;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.procedure.CanonicalStoredProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GrupoRepositorySqlServerAdapterTest {

    private static final UUID TIPO_IDENTIFICACION = UUID.fromString("13641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID GRUPO = UUID.fromString("23641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID CORRELACION = UUID.fromString("93641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID ASIGNATURA = UUID.fromString("33641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID DOCENTE = UUID.fromString("43641bab-e3cd-485c-b275-47e7b731e18c");

    @AfterEach
    void clearCorrelationContext() {
        CorrelationIdContext.clear();
    }

    @Test
    void registrarEstudiante_envia_parametros_exactos_al_ejecutor_canonico() {
        final CanonicalStoredProcedureExecutor procedureExecutor = mock(CanonicalStoredProcedureExecutor.class);
        final NamedParameterJdbcOperations jdbcOperations = mock(NamedParameterJdbcOperations.class);
        when(procedureExecutor.execute(anyString(), anyString(), any(MapSqlParameterSource.class)))
                .thenReturn(new CanonicalProcedureResult(CORRELACION, "Estudiante registrado.", "detalle interno", true));
        final AtomicBoolean transaccionEjecutada = new AtomicBoolean(false);
        final GrupoRepositorySqlServerAdapter adapter = new GrupoRepositorySqlServerAdapter(
                procedureExecutor,
                jdbcOperations,
                transactionOperations(transaccionEjecutada)
        );
        CorrelationIdContext.set(CORRELACION);

        final RegistrarEstudianteRepositoryProjection resultado = adapter.registrarEstudianteEnGrupo(dtoValido());

        final ArgumentCaptor<String> operationCaptor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<MapSqlParameterSource> parametersCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(procedureExecutor).execute(operationCaptor.capture(), sqlCaptor.capture(), parametersCaptor.capture());
        final MapSqlParameterSource parametros = parametersCaptor.getValue();

        assertTrue(transaccionEjecutada.get());
        assertEquals("registrarEstudianteEnGrupo", operationCaptor.getValue());
        assertTrue(sqlCaptor.getValue().contains("dbo.usp_registrar_estudiante_en_grupo_usuario_no_existente"));
        assertTrue(sqlCaptor.getValue().contains("@idTipoIdIdentificacion = :idTipoIdIdentificacion"));
        assertEquals(TIPO_IDENTIFICACION, parametros.getValue(GrupoRepositorySqlServerAdapter.PARAM_ID_TIPO_ID_IDENTIFICACION));
        assertEquals(123456789, parametros.getValue(GrupoRepositorySqlServerAdapter.PARAM_NUMERO_IDENTIFICACION));
        assertEquals("PEREZ", parametros.getValue(GrupoRepositorySqlServerAdapter.PARAM_PRIMER_APELLIDO));
        assertEquals("GOMEZ", parametros.getValue(GrupoRepositorySqlServerAdapter.PARAM_SEGUNDO_APELLIDO));
        assertEquals("ANA", parametros.getValue(GrupoRepositorySqlServerAdapter.PARAM_PRIMER_NOMBRE));
        assertEquals("MARIA", parametros.getValue(GrupoRepositorySqlServerAdapter.PARAM_SEGUNDO_NOMBRE));
        assertEquals("ana.perez@uco.edu.co", parametros.getValue(GrupoRepositorySqlServerAdapter.PARAM_CORREO));
        assertEquals("Clave123!", parametros.getValue(GrupoRepositorySqlServerAdapter.PARAM_PASSWORD));
        assertEquals(GRUPO, parametros.getValue(GrupoRepositorySqlServerAdapter.PARAM_ID_GRUPO));
        assertEquals(CORRELACION, parametros.getValue(GrupoRepositorySqlServerAdapter.PARAM_ID_CORRELACION));
        assertEquals(10, parametros.getValues().size());
        assertEquals("Estudiante registrado.", resultado.getMensajeUsuario());
    }

    @Test
    void registrarEstudiante_propaga_error_de_negocio_del_ejecutor_canonico() {
        final CanonicalStoredProcedureExecutor procedureExecutor = mock(CanonicalStoredProcedureExecutor.class);
        final GrupoRepositorySqlServerAdapter adapter = new GrupoRepositorySqlServerAdapter(
                procedureExecutor,
                mock(NamedParameterJdbcOperations.class),
                transactionOperations(new AtomicBoolean(false))
        );
        when(procedureExecutor.execute(anyString(), anyString(), any(MapSqlParameterSource.class)))
                .thenThrow(new ConflictException(GrupoErrorCode.ERR_MATRICULA_DUPLICADA));
        CorrelationIdContext.set(CORRELACION);

        final ConflictException exception = assertThrows(
                ConflictException.class,
                () -> adapter.registrarEstudianteEnGrupo(dtoValido())
        );

        assertEquals("ERR_MATRICULA_DUPLICADA", exception.getCode());
        assertEquals(GrupoErrorCode.ERR_MATRICULA_DUPLICADA.defaultMessage(), exception.getMessage());
    }

    @Test
    void registrarEstudiante_propaga_error_tecnico_del_ejecutor_canonico() {
        final CanonicalStoredProcedureExecutor procedureExecutor = mock(CanonicalStoredProcedureExecutor.class);
        final GrupoRepositorySqlServerAdapter adapter = new GrupoRepositorySqlServerAdapter(
                procedureExecutor,
                mock(NamedParameterJdbcOperations.class),
                transactionOperations(new AtomicBoolean(false))
        );
        when(procedureExecutor.execute(anyString(), anyString(), any(MapSqlParameterSource.class)))
                .thenThrow(new DatabaseOperationException(
                        DatabaseErrorCode.ERR_DB_UNCLASSIFIED,
                        DatabaseErrorCode.ERR_DB_UNCLASSIFIED.defaultMessage()
                ));
        CorrelationIdContext.set(CORRELACION);

        final DatabaseOperationException exception = assertThrows(
                DatabaseOperationException.class,
                () -> adapter.registrarEstudianteEnGrupo(dtoValido())
        );

        assertEquals("ERR_DB_UNCLASSIFIED", exception.getCode());
        assertEquals(DatabaseErrorCode.ERR_DB_UNCLASSIFIED.defaultMessage(), exception.getMessage());
    }

    @Test
    void errores_jdbc_en_consultarGrupos_se_convierten_en_databaseOperationException() {
        final NamedParameterJdbcOperations jdbcOperations = mock(NamedParameterJdbcOperations.class);
        when(jdbcOperations.query(anyString(), any(RowMapper.class)))
                .thenThrow(new DataAccessResourceFailureException("fallo tecnico"));
        final GrupoRepositorySqlServerAdapter adapter = new GrupoRepositorySqlServerAdapter(
                mock(CanonicalStoredProcedureExecutor.class),
                jdbcOperations,
                transactionOperations(new AtomicBoolean(false))
        );

        assertThrows(DatabaseOperationException.class, adapter::consultarGrupos);
    }

    @Test
    void registrarEstudiante_sin_contexto_no_genera_correlationId_nuevo() {
        final AtomicBoolean spEjecutado = new AtomicBoolean(false);
        final CanonicalStoredProcedureExecutor procedureExecutor = mock(CanonicalStoredProcedureExecutor.class);
        when(procedureExecutor.execute(anyString(), anyString(), any(MapSqlParameterSource.class)))
                .thenAnswer(invocation -> {
                    spEjecutado.set(true);
                    return new CanonicalProcedureResult(CORRELACION, "ok", "ok", true);
                });
        final GrupoRepositorySqlServerAdapter adapter = new GrupoRepositorySqlServerAdapter(
                procedureExecutor,
                mock(NamedParameterJdbcOperations.class),
                transactionOperations(new AtomicBoolean(false))
        );

        assertThrows(CrosscuttingException.class, () -> adapter.registrarEstudianteEnGrupo(dtoValido()));
        assertFalse(spEjecutado.get());
        assertEquals(null, CorrelationIdContext.get());
    }

    @Test
    void consultarGrupos_usa_vista_uv_grupo_y_mapea_columnas_confirmadas() {
        final NamedParameterJdbcOperations jdbcOperations = mock(NamedParameterJdbcOperations.class);
        final GrupoRepositoryProjection entity = new GrupoRepositoryProjection(
                GRUPO,
                "G1",
                "Grupo 1",
                ASIGNATURA,
                "Backend",
                DOCENTE,
                30,
                12,
                18,
                true,
                LocalDate.of(2026, 1, 20),
                LocalDate.of(2026, 5, 30)
        );
        when(jdbcOperations.query(anyString(), any(RowMapper.class))).thenReturn(List.of(entity));
        final GrupoRepositorySqlServerAdapter adapter = new GrupoRepositorySqlServerAdapter(
                mock(CanonicalStoredProcedureExecutor.class),
                jdbcOperations,
                transactionOperations(new AtomicBoolean(false))
        );

        final List<GrupoRepositoryProjection> resultado = adapter.consultarGrupos();

        final ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcOperations).query(sqlCaptor.capture(), any(RowMapper.class));
        assertEquals(1, resultado.size());
        assertTrue(sqlCaptor.getValue().contains("FROM dbo.uv_grupo"));
        assertFalse(sqlCaptor.getValue().toUpperCase().contains("SELECT *"));
        assertEquals("Backend", resultado.getFirst().getNombreAsignatura());
    }

    @Test
    void grupoRepositoryRowMapper_convierte_tipos_de_sql_server() throws SQLException {
        final ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getObject("id")).thenReturn(GRUPO.toString());
        when(resultSet.getString("codigo")).thenReturn("G1");
        when(resultSet.getString("nombre")).thenReturn("Grupo 1");
        when(resultSet.getObject("idAsignatura")).thenReturn(ASIGNATURA);
        when(resultSet.getString("nombreAsignatura")).thenReturn("Backend");
        when(resultSet.getObject("idDocente")).thenReturn(DOCENTE.toString());
        when(resultSet.getObject("capacidadMaximaPermitida")).thenReturn(30L);
        when(resultSet.getObject("estudiantesActivos")).thenReturn("12");
        when(resultSet.getObject("cuposDisponibles")).thenReturn(18);
        when(resultSet.getObject("grupoEstaHablitado")).thenReturn(1);
        when(resultSet.getObject("fechaInicioPeriodoAcademico")).thenReturn(Timestamp.valueOf(
                LocalDateTime.of(2026, 1, 20, 8, 0)
        ));
        when(resultSet.getObject("fechaFinPeriodoAcademico")).thenReturn(java.sql.Date.valueOf("2026-05-30"));

        final GrupoRepositoryProjection entity = new GrupoRepositoryRowMapper().mapRow(resultSet, 0);

        assertEquals(GRUPO, entity.getId());
        assertEquals(ASIGNATURA, entity.getIdAsignatura());
        assertEquals(DOCENTE, entity.getIdDocente());
        assertEquals(30, entity.getCapacidadMaximaPermitida());
        assertEquals(12, entity.getEstudiantesActivos());
        assertTrue(entity.isGrupoHabilitado());
        assertEquals(LocalDate.of(2026, 1, 20), entity.getFechaInicioPeriodoAcademico());
        assertEquals(LocalDate.of(2026, 5, 30), entity.getFechaFinPeriodoAcademico());
    }

    private RegistrarEstudianteRepositoryDTO dtoValido() {
        return new RegistrarEstudianteRepositoryDTO(
                TIPO_IDENTIFICACION,
                123456789,
                "PEREZ",
                "GOMEZ",
                "ANA",
                "MARIA",
                "ana.perez@uco.edu.co",
                "Clave123!",
                GRUPO
        );
    }

    private TransactionOperations transactionOperations(final AtomicBoolean transaccionEjecutada) {
        return new TransactionOperations() {
            @Override
            public <T> T execute(final TransactionCallback<T> action) {
                transaccionEjecutada.set(true);
                return action.doInTransaction(mock(TransactionStatus.class));
            }
        };
    }
}
