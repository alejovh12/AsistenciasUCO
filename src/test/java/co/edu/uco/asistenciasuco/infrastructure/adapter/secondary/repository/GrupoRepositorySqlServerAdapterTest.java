package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository;

import co.edu.uco.asistenciasuco.application.exception.DatabaseOperationException;
import co.edu.uco.asistenciasuco.application.exception.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.ErrorCode;
import co.edu.uco.asistenciasuco.application.exception.InternalApplicationException;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarEstudianteRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.GrupoRepositoryEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.RegistrarEstudianteRepositoryEntity;
import co.edu.uco.asistenciasuco.infrastructure.correlation.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
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
    void registrarEstudiante_envia_parametros_exactos_y_usa_misma_correlacion_del_contexto() {
        final AtomicReference<String> sqlCapturado = new AtomicReference<>();
        final AtomicReference<MapSqlParameterSource> parametrosCapturados = new AtomicReference<>();
        final AtomicBoolean transaccionEjecutada = new AtomicBoolean(false);
        final GrupoRepositorySqlServerAdapter adapter = new GrupoRepositorySqlServerAdapter(
                (sql, parameters) -> {
                    sqlCapturado.set(sql);
                    parametrosCapturados.set(parameters);
                    return Map.of(
                            GrupoRepositorySqlServerAdapter.OUT_ESTADO, true,
                            GrupoRepositorySqlServerAdapter.OUT_MENSAJE_USUARIO, "Estudiante registrado.",
                            GrupoRepositorySqlServerAdapter.OUT_MENSAJE_TECNICO, "detalle interno"
                    );
                },
                (sql, rowMapper) -> List.of(),
                transactionOperations(transaccionEjecutada)
        );
        CorrelationIdContext.set(CORRELACION);

        final RegistrarEstudianteRepositoryEntity resultado = adapter.registrarEstudianteEnGrupo(dtoValido());

        final Map<String, Object> values = parametrosCapturados.get().getValues();
        assertTrue(transaccionEjecutada.get());
        assertTrue(sqlCapturado.get().contains("dbo.usp_registrar_estudiante_en_grupo_usuario_no_existente"));
        assertTrue(sqlCapturado.get().contains("@idTipoIdIdentificacion = :idTipoIdIdentificacion"));
        assertEquals(TIPO_IDENTIFICACION.toString(), values.get(GrupoRepositorySqlServerAdapter.PARAM_ID_TIPO_ID_IDENTIFICACION));
        assertEquals(123456789, values.get(GrupoRepositorySqlServerAdapter.PARAM_NUMERO_IDENTIFICACION));
        assertEquals("PEREZ", values.get(GrupoRepositorySqlServerAdapter.PARAM_PRIMER_APELLIDO));
        assertEquals("GOMEZ", values.get(GrupoRepositorySqlServerAdapter.PARAM_SEGUNDO_APELLIDO));
        assertEquals("ANA", values.get(GrupoRepositorySqlServerAdapter.PARAM_PRIMER_NOMBRE));
        assertEquals("MARIA", values.get(GrupoRepositorySqlServerAdapter.PARAM_SEGUNDO_NOMBRE));
        assertEquals("ana.perez@uco.edu.co", values.get(GrupoRepositorySqlServerAdapter.PARAM_CORREO));
        assertEquals("Clave123!", values.get(GrupoRepositorySqlServerAdapter.PARAM_PASSWORD));
        assertEquals(GRUPO.toString(), values.get(GrupoRepositorySqlServerAdapter.PARAM_ID_GRUPO));
        assertEquals(CORRELACION.toString(), values.get(GrupoRepositorySqlServerAdapter.PARAM_ID_CORRELACION));
        assertEquals(10, values.size());
        assertEquals("Estudiante registrado.", resultado.getMensajeUsuario());
    }

    @Test
    void registrarEstudiante_con_error_conocido_retorna_codigo_err_y_excepcion_correcta() {
        final GrupoRepositorySqlServerAdapter adapter = new GrupoRepositorySqlServerAdapter(
                (sql, parameters) -> Map.of(
                        GrupoRepositorySqlServerAdapter.OUT_ESTADO, false,
                        GrupoRepositorySqlServerAdapter.OUT_MENSAJE_USUARIO, "La matricula ya se encuentra registrada.",
                        GrupoRepositorySqlServerAdapter.OUT_MENSAJE_TECNICO, "SQLException password stacktrace"
                ),
                (sql, rowMapper) -> List.of(),
                transactionOperations(new AtomicBoolean(false))
        );
        CorrelationIdContext.set(CORRELACION);

        final ConflictException exception = assertThrows(
                ConflictException.class,
                () -> adapter.registrarEstudianteEnGrupo(dtoValido())
        );

        assertEquals("ERR_MATRICULA_DUPLICADA", exception.getCode());
        assertEquals(ErrorCode.ERR_MATRICULA_DUPLICADA.defaultMessage(), exception.getMessage());
    }

    @Test
    void registrarEstudiante_con_error_desconocido_retorna_error_interno_seguro() {
        final GrupoRepositorySqlServerAdapter adapter = new GrupoRepositorySqlServerAdapter(
                (sql, parameters) -> Map.of(
                        GrupoRepositorySqlServerAdapter.OUT_ESTADO, false,
                        GrupoRepositorySqlServerAdapter.OUT_MENSAJE_USUARIO, "mensaje publico no clasificado",
                        GrupoRepositorySqlServerAdapter.OUT_MENSAJE_TECNICO, "SQLException password stacktrace"
                ),
                (sql, rowMapper) -> List.of(),
                transactionOperations(new AtomicBoolean(false))
        );
        CorrelationIdContext.set(CORRELACION);

        final InternalApplicationException exception = assertThrows(
                InternalApplicationException.class,
                () -> adapter.registrarEstudianteEnGrupo(dtoValido())
        );

        assertEquals("ERR_DB_UNCLASSIFIED", exception.getCode());
        assertEquals(ErrorCode.ERR_DB_UNCLASSIFIED.defaultMessage(), exception.getMessage());
    }

    @Test
    void errores_dataAccess_se_convierten_en_databaseOperationException() {
        final GrupoRepositorySqlServerAdapter adapter = new GrupoRepositorySqlServerAdapter(
                (sql, parameters) -> {
                    throw new DataAccessResourceFailureException("fallo tecnico");
                },
                (sql, rowMapper) -> {
                    throw new DataAccessResourceFailureException("fallo tecnico");
                },
                transactionOperations(new AtomicBoolean(false))
        );
        CorrelationIdContext.set(CORRELACION);

        assertThrows(DatabaseOperationException.class, () -> adapter.registrarEstudianteEnGrupo(dtoValido()));
        assertThrows(DatabaseOperationException.class, adapter::consultarGrupos);
    }

    @Test
    void registrarEstudiante_sin_contexto_no_genera_correlationId_nuevo() {
        final AtomicBoolean spEjecutado = new AtomicBoolean(false);
        final GrupoRepositorySqlServerAdapter adapter = new GrupoRepositorySqlServerAdapter(
                (sql, parameters) -> {
                    spEjecutado.set(true);
                    return Map.of();
                },
                (sql, rowMapper) -> List.of(),
                transactionOperations(new AtomicBoolean(false))
        );

        assertThrows(CrosscuttingException.class, () -> adapter.registrarEstudianteEnGrupo(dtoValido()));
        assertFalse(spEjecutado.get());
        assertEquals(null, CorrelationIdContext.get());
    }

    @Test
    void consultarGrupos_usa_vista_uv_grupo_y_mapea_columnas_confirmadas() {
        final AtomicReference<String> sqlCapturado = new AtomicReference<>();
        final GrupoRepositoryEntity entity = new GrupoRepositoryEntity(
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
        final GrupoRepositorySqlServerAdapter adapter = new GrupoRepositorySqlServerAdapter(
                (sql, parameters) -> Map.of(),
                (sql, rowMapper) -> {
                    sqlCapturado.set(sql);
                    return List.of(entity);
                },
                transactionOperations(new AtomicBoolean(false))
        );

        final List<GrupoRepositoryEntity> resultado = adapter.consultarGrupos();

        assertEquals(1, resultado.size());
        assertTrue(sqlCapturado.get().contains("FROM dbo.uv_grupo"));
        assertFalse(sqlCapturado.get().toUpperCase().contains("SELECT *"));
        assertEquals("Backend", resultado.getFirst().getNombreAsignatura());
    }

    @Test
    void mapGrupoRow_convierte_tipos_de_sql_server() throws SQLException {
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

        final GrupoRepositoryEntity entity = GrupoRepositorySqlServerAdapter.mapGrupoRow(resultSet, 0);

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
