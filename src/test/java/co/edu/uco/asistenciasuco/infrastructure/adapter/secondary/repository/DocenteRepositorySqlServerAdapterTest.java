package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository;

import co.edu.uco.asistenciasuco.application.exception.DatabaseOperationException;
import co.edu.uco.asistenciasuco.application.exception.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.AsignarDocenteAGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsignacionesAcademicasDocenteRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarDocentePorIdRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarDocenteDesdeUsuarioRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.DocenteAsignacionAcademicaRepositoryEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.DocenteIdentidadRepositoryEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.DocenteOperacionRepositoryEntity;
import co.edu.uco.asistenciasuco.infrastructure.correlation.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocenteRepositorySqlServerAdapterTest {

    private static final UUID DOCENTE = UUID.fromString("13641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID USUARIO = UUID.fromString("23641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID GRUPO = UUID.fromString("33641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID CORRELACION = UUID.fromString("93641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID CORRELACION_CONTEXTO = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @AfterEach
    void clearCorrelationContext() {
        CorrelationIdContext.clear();
    }

    @Test
    void consultarDocentes_usa_vista_identidad_columnas_explicitas_y_orden() {
        final AtomicReference<String> sqlCapturado = new AtomicReference<>();
        final DocenteRepositorySqlServerAdapter adapter = adapterBase(
                sql -> {
                    sqlCapturado.set(sql);
                    return List.of();
                },
                (sql, args) -> List.of(),
                (sql, args) -> List.of(),
                parameters -> output(true, "ok", "detalle"),
                parameters -> output(true, "ok", "detalle")
        );

        assertTrue(adapter.consultarDocentes().isEmpty());
        final String sql = sqlCapturado.get();
        assertFalse(sql.toUpperCase().contains("SELECT *"));
        assertTrue(sql.contains("FROM dbo.uv_docente_identidad"));
        assertTrue(sql.contains("ORDER BY nombreCompleto, id"));
    }

    @Test
    void consultarDocentePorId_filtra_por_id_y_retorna_vacio_cuando_no_existe() {
        final AtomicReference<String> sqlCapturado = new AtomicReference<>();
        final AtomicReference<Object[]> argsCapturados = new AtomicReference<>();
        final DocenteRepositorySqlServerAdapter adapter = adapterBase(
                sql -> List.of(),
                (sql, args) -> {
                    sqlCapturado.set(sql);
                    argsCapturados.set(args);
                    return List.of();
                },
                (sql, args) -> List.of(),
                parameters -> output(true, "ok", "detalle"),
                parameters -> output(true, "ok", "detalle")
        );

        final Optional<DocenteIdentidadRepositoryEntity> resultado =
                adapter.consultarDocentePorId(new ConsultarDocentePorIdRepositoryDTO(DOCENTE));

        assertTrue(resultado.isEmpty());
        assertTrue(sqlCapturado.get().contains("FROM dbo.uv_docente_identidad"));
        assertTrue(sqlCapturado.get().contains("WHERE id = ?"));
        assertEquals(DOCENTE.toString(), argsCapturados.get()[0]);
    }

    @Test
    void consultarAsignacionesAcademicas_usa_uv_docente_y_permite_varias_filas() {
        final AtomicReference<String> sqlCapturado = new AtomicReference<>();
        final DocenteRepositorySqlServerAdapter adapter = adapterBase(
                sql -> List.of(),
                (sql, args) -> List.of(),
                (sql, args) -> {
                    sqlCapturado.set(sql);
                    return List.of(asignacion(1), asignacion(1));
                },
                parameters -> output(true, "ok", "detalle"),
                parameters -> output(true, "ok", "detalle")
        );

        final List<DocenteAsignacionAcademicaRepositoryEntity> resultado =
                adapter.consultarAsignacionesAcademicas(
                        new ConsultarAsignacionesAcademicasDocenteRepositoryDTO(DOCENTE)
                );

        assertEquals(2, resultado.size());
        assertTrue(sqlCapturado.get().contains("FROM dbo.uv_docente"));
        assertTrue(sqlCapturado.get().contains("ORDER BY"));
        assertTrue(sqlCapturado.get().contains("idGrupo"));
    }

    @Test
    void procedimientos_envian_parametros_exactos_y_leen_outputs_sin_exponer_tecnico_en_resultado_publico() {
        final AtomicReference<MapSqlParameterSource> registrarParams = new AtomicReference<>();
        final AtomicReference<MapSqlParameterSource> asignarParams = new AtomicReference<>();
        final DocenteRepositorySqlServerAdapter adapter = adapterBase(
                sql -> List.of(),
                (sql, args) -> List.of(),
                (sql, args) -> List.of(),
                parameters -> {
                    registrarParams.set(parameters);
                    return output(true, "Docente registrado.", "detalle interno");
                },
                parameters -> {
                    asignarParams.set(parameters);
                    return output(true, "Docente asignado.", "detalle interno");
                }
        );

        CorrelationIdContext.set(CORRELACION);

        final DocenteOperacionRepositoryEntity registro = adapter.registrarDocenteDesdeUsuario(
                new RegistrarDocenteDesdeUsuarioRepositoryDTO(USUARIO)
        );
        final DocenteOperacionRepositoryEntity asignacion = adapter.asignarDocenteAGrupo(
                new AsignarDocenteAGrupoRepositoryDTO(DOCENTE, GRUPO)
        );

        assertEquals(USUARIO.toString(), registrarParams.get().getValues().get(DocenteRepositorySqlServerAdapter.PARAM_ID_USUARIO));
        assertEquals(CORRELACION.toString(), registrarParams.get().getValues().get(DocenteRepositorySqlServerAdapter.PARAM_ID_CORRELACION));
        assertEquals(DOCENTE.toString(), asignarParams.get().getValues().get(DocenteRepositorySqlServerAdapter.PARAM_ID_DOCENTE));
        assertEquals(GRUPO.toString(), asignarParams.get().getValues().get(DocenteRepositorySqlServerAdapter.PARAM_ID_GRUPO));
        assertEquals("Docente registrado.", registro.getMensajeUsuario());
        assertEquals("Docente asignado.", asignacion.getMensajeUsuario());
    }

    @Test
    void procedimiento_con_estado_fallido_traduce_mensaje_resultado_a_excepcion() {
        final DocenteRepositorySqlServerAdapter adapter = adapterBase(
                sql -> List.of(),
                (sql, args) -> List.of(),
                (sql, args) -> List.of(),
                parameters -> output(false, "El docente no existe.", "detalle sensible"),
                parameters -> output(true, "ok", "detalle")
        );
        CorrelationIdContext.set(CORRELACION);

        final ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> adapter.registrarDocenteDesdeUsuario(
                        new RegistrarDocenteDesdeUsuarioRepositoryDTO(USUARIO)
                )
        );

        assertEquals("ERR_DOCENTE_NO_EXISTE", exception.getCode());
        assertEquals("El docente no existe.", exception.getMessage());
    }

    @Test
    void procedimiento_usa_correlationId_de_contexto_para_parametro_sql() {
        final AtomicReference<MapSqlParameterSource> registrarParams = new AtomicReference<>();
        final DocenteRepositorySqlServerAdapter adapter = adapterBase(
                sql -> List.of(),
                (sql, args) -> List.of(),
                (sql, args) -> List.of(),
                parameters -> {
                    registrarParams.set(parameters);
                    return output(true, "Docente registrado.", "detalle interno");
                },
                parameters -> output(true, "ok", "detalle")
        );
        CorrelationIdContext.set(CORRELACION_CONTEXTO);

        adapter.registrarDocenteDesdeUsuario(new RegistrarDocenteDesdeUsuarioRepositoryDTO(USUARIO));

        assertEquals(
                CORRELACION_CONTEXTO.toString(),
                registrarParams.get().getValues().get(DocenteRepositorySqlServerAdapter.PARAM_ID_CORRELACION)
        );
    }

    @Test
    void convierte_dataAccessException_en_databaseOperationException() {
        final DocenteRepositorySqlServerAdapter adapter = adapterBase(
                sql -> {
                    throw new DataAccessResourceFailureException("fallo");
                },
                (sql, args) -> {
                    throw new DataAccessResourceFailureException("fallo");
                },
                (sql, args) -> {
                    throw new DataAccessResourceFailureException("fallo");
                },
                parameters -> {
                    throw new DataAccessResourceFailureException("fallo");
                },
                parameters -> {
                    throw new DataAccessResourceFailureException("fallo");
                }
        );
        CorrelationIdContext.set(CORRELACION);

        assertThrows(DatabaseOperationException.class, adapter::consultarDocentes);
        assertThrows(
                DatabaseOperationException.class,
                () -> adapter.consultarDocentePorId(new ConsultarDocentePorIdRepositoryDTO(DOCENTE))
        );
        assertThrows(
                DatabaseOperationException.class,
                () -> adapter.consultarAsignacionesAcademicas(
                        new ConsultarAsignacionesAcademicasDocenteRepositoryDTO(DOCENTE)
                )
        );
        assertThrows(
                DatabaseOperationException.class,
                () -> adapter.registrarDocenteDesdeUsuario(new RegistrarDocenteDesdeUsuarioRepositoryDTO(USUARIO))
        );
        assertThrows(
                DatabaseOperationException.class,
                () -> adapter.asignarDocenteAGrupo(new AsignarDocenteAGrupoRepositoryDTO(DOCENTE, GRUPO))
        );
    }

    @Test
    void procedimiento_sin_contexto_no_genera_correlationId_nuevo() {
        final AtomicReference<MapSqlParameterSource> registrarParams = new AtomicReference<>();
        final DocenteRepositorySqlServerAdapter adapter = adapterBase(
                sql -> List.of(),
                (sql, args) -> List.of(),
                (sql, args) -> List.of(),
                parameters -> {
                    registrarParams.set(parameters);
                    return output(true, "ok", "detalle");
                },
                parameters -> output(true, "ok", "detalle")
        );

        assertThrows(
                CrosscuttingException.class,
                () -> adapter.registrarDocenteDesdeUsuario(new RegistrarDocenteDesdeUsuarioRepositoryDTO(USUARIO))
        );
        assertEquals(null, registrarParams.get());
        assertEquals(null, CorrelationIdContext.get());
    }

    @Test
    void mapRows_mapean_todas_las_columnas() throws SQLException {
        final ResultSet identidad = mock(ResultSet.class);
        when(identidad.getObject("id")).thenReturn(DOCENTE);
        when(identidad.getObject("idUsuario")).thenReturn(USUARIO.toString());
        when(identidad.getObject("numeroIdentificacion")).thenReturn(123456789);
        when(identidad.getString("nombreCompleto")).thenReturn("Ana Perez");
        when(identidad.getObject("estaActivoUsuario")).thenReturn(1);

        final DocenteIdentidadRepositoryEntity identidadEntity =
                DocenteRepositorySqlServerAdapter.mapIdentidadRow(identidad, 0);

        assertEquals(DOCENTE, identidadEntity.getId());
        assertEquals(USUARIO, identidadEntity.getIdUsuario());
        assertTrue(identidadEntity.isEstaActivoUsuario());

        final ResultSet asignacion = mockAsignacionResultSet();
        final DocenteAsignacionAcademicaRepositoryEntity asignacionEntity =
                DocenteRepositorySqlServerAdapter.mapAsignacionRow(asignacion, 0);

        assertEquals("UCO", asignacionEntity.getNombreInstitucion());
        assertEquals("Backend", asignacionEntity.getNombreAsignatura());
        assertEquals(1, asignacionEntity.getEstaActivoDocente());
    }

    private DocenteRepositorySqlServerAdapter adapterBase(
            final DocenteRepositorySqlServerAdapter.QueryExecutor<DocenteIdentidadRepositoryEntity> consultarDocentes,
            final DocenteRepositorySqlServerAdapter.ParameterizedQueryExecutor<DocenteIdentidadRepositoryEntity> consultarPorId,
            final DocenteRepositorySqlServerAdapter.ParameterizedQueryExecutor<DocenteAsignacionAcademicaRepositoryEntity> consultarAsignaciones,
            final DocenteRepositorySqlServerAdapter.ProcedureExecutor registrar,
            final DocenteRepositorySqlServerAdapter.ProcedureExecutor asignar
    ) {
        return new DocenteRepositorySqlServerAdapter(
                consultarDocentes,
                consultarPorId,
                consultarAsignaciones,
                registrar,
                asignar
        );
    }

    private Map<String, Object> output(final boolean estado, final String usuario, final String tecnico) {
        final Map<String, Object> output = new HashMap<>();
        output.put(DocenteRepositorySqlServerAdapter.OUT_ESTADO, estado);
        output.put(DocenteRepositorySqlServerAdapter.OUT_MENSAJE_USUARIO, usuario);
        output.put(DocenteRepositorySqlServerAdapter.OUT_MENSAJE_TECNICO, tecnico);
        return output;
    }

    private DocenteAsignacionAcademicaRepositoryEntity asignacion(final Integer estadoDocente) {
        return new DocenteAsignacionAcademicaRepositoryEntity(
                DOCENTE,
                USUARIO,
                123456789,
                "Ana Perez",
                true,
                UUID.fromString("43641bab-e3cd-485c-b275-47e7b731e18c"),
                "UCO",
                UUID.fromString("53641bab-e3cd-485c-b275-47e7b731e18c"),
                "Ingenieria",
                UUID.fromString("63641bab-e3cd-485c-b275-47e7b731e18c"),
                "Sistemas",
                UUID.fromString("73641bab-e3cd-485c-b275-47e7b731e18c"),
                "2024",
                UUID.fromString("83641bab-e3cd-485c-b275-47e7b731e18c"),
                "Backend",
                GRUPO,
                "G1",
                UUID.fromString("93641bab-e3cd-485c-b275-47e7b731e18b"),
                "DO",
                "Docente",
                estadoDocente,
                "ACTIVO"
        );
    }

    private ResultSet mockAsignacionResultSet() throws SQLException {
        final ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getObject("id")).thenReturn(DOCENTE);
        when(resultSet.getObject("idUsuario")).thenReturn(USUARIO);
        when(resultSet.getObject("numeroIdentificacion")).thenReturn(123456789);
        when(resultSet.getString("nombreCompleto")).thenReturn("Ana Perez");
        when(resultSet.getObject("estaActivoUsuario")).thenReturn(Boolean.TRUE);
        when(resultSet.getObject("idInstitucion")).thenReturn(UUID.fromString("43641bab-e3cd-485c-b275-47e7b731e18c"));
        when(resultSet.getString("nombreInstitucion")).thenReturn("UCO");
        when(resultSet.getObject("idFacultad")).thenReturn(UUID.fromString("53641bab-e3cd-485c-b275-47e7b731e18c"));
        when(resultSet.getString("nombreFacultad")).thenReturn("Ingenieria");
        when(resultSet.getObject("idPrograma")).thenReturn(UUID.fromString("63641bab-e3cd-485c-b275-47e7b731e18c"));
        when(resultSet.getString("nombrePrograma")).thenReturn("Sistemas");
        when(resultSet.getObject("idPlanEstudio")).thenReturn(UUID.fromString("73641bab-e3cd-485c-b275-47e7b731e18c"));
        when(resultSet.getString("inpPlanEstudio")).thenReturn("2024");
        when(resultSet.getObject("idAsignatura")).thenReturn(UUID.fromString("83641bab-e3cd-485c-b275-47e7b731e18c"));
        when(resultSet.getString("nombreAsignatura")).thenReturn("Backend");
        when(resultSet.getObject("idGrupo")).thenReturn(GRUPO);
        when(resultSet.getString("nombreGrupo")).thenReturn("G1");
        when(resultSet.getObject("idPerfil")).thenReturn(UUID.fromString("93641bab-e3cd-485c-b275-47e7b731e18b"));
        when(resultSet.getString("codigoPerfil")).thenReturn("DO");
        when(resultSet.getString("nombrePerfil")).thenReturn("Docente");
        when(resultSet.getObject("estaActivoDocente")).thenReturn(1);
        when(resultSet.getString("estaActivoTextoDocente")).thenReturn("ACTIVO");
        return resultSet;
    }
}
