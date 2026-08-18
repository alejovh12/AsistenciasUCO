package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository;

import co.edu.uco.asistenciasuco.application.exception.DatabaseOperationException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarEstudiantesRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.EstudianteDetalleRepositoryEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.EstudiantePaginaRepositoryEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.EstudianteResumenRepositoryEntity;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EstudianteRepositorySqlServerAdapterTest {

    private static final UUID ESTUDIANTE = UUID.fromString("13641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID USUARIO = UUID.fromString("23641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID TIPO_IDENTIFICACION = UUID.fromString("33641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID INSTITUCION = UUID.fromString("43641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID FACULTAD = UUID.fromString("53641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID PROGRAMA = UUID.fromString("63641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID PLAN = UUID.fromString("73641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID ASIGNATURA = UUID.fromString("83641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID GRUPO = UUID.fromString("93641bab-e3cd-485c-b275-47e7b731e18c");

    @Test
    void consultarEstudiantes_usa_sql_parametrizado_paginado_y_exists_para_filtros_academicos() {
        final NamedParameterJdbcOperations jdbcOperations = mock(NamedParameterJdbcOperations.class);
        final AtomicReference<String> countSql = new AtomicReference<>();
        final AtomicReference<String> listSql = new AtomicReference<>();
        final AtomicReference<SqlParameterSource> listParams = new AtomicReference<>();

        when(jdbcOperations.queryForObject(anyString(), any(SqlParameterSource.class), eq(Number.class)))
                .thenAnswer(invocation -> {
                    countSql.set(invocation.getArgument(0));
                    return 11L;
                });
        when(jdbcOperations.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    listSql.set(invocation.getArgument(0));
                    listParams.set(invocation.getArgument(1));
                    final RowMapper<EstudianteResumenRepositoryEntity> rowMapper = invocation.getArgument(2);
                    return List.of(rowMapper.mapRow(resumenResultSet(), 0));
                });
        final EstudianteRepositorySqlServerAdapter adapter = new EstudianteRepositorySqlServerAdapter(jdbcOperations);

        final EstudiantePaginaRepositoryEntity page = adapter.consultarEstudiantes(new ConsultarEstudiantesRepositoryDTO(
                TIPO_IDENTIFICACION,
                123456789,
                "ana",
                "ana.perez@uco.edu.co",
                INSTITUCION,
                FACULTAD,
                PROGRAMA,
                GRUPO,
                true,
                1,
                10
        ));

        assertEquals(1, page.items().size());
        assertEquals(11, page.totalItems());
        assertEquals(2, page.totalPages());
        assertEquals(1, page.page());
        assertEquals(10, page.size());
        assertTrue(countSql.get().contains("FROM dbo.uv_estudiante_identidad e"));
        assertTrue(countSql.get().contains("INNER JOIN dbo.uv_usuario u ON e.idUsuario = u.id"));
        assertTrue(listSql.get().contains("u.idTipoIdentificacion AS tipoIdentificacionId"));
        assertTrue(listSql.get().contains("u.estaActivoUsuario AS estaActivoUsuario"));
        assertTrue(listSql.get().contains("u.idTipoIdentificacion = :tipoIdentificacionId"));
        assertTrue(listSql.get().contains("u.estaActivoUsuario = :activo"));
        assertFalse(listSql.get().contains("u.tipoIdentificacion"));
        assertFalse(listSql.get().contains("u.estado"));
        assertTrue(listSql.get().contains("OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY"));
        assertTrue(listSql.get().contains("EXISTS"));
        assertTrue(listSql.get().contains("FROM dbo.uv_estudiante academico"));
        assertFalse(listSql.get().contains("SELECT *"));
        assertEquals(10, listParams.get().getValue("offset"));
        assertEquals(10, listParams.get().getValue("size"));
        assertEquals(TIPO_IDENTIFICACION.toString(), listParams.get().getValue("tipoIdentificacionId"));
        assertEquals(INSTITUCION.toString(), listParams.get().getValue("institucionId"));
        assertEquals(GRUPO.toString(), listParams.get().getValue("grupoId"));
    }

    @Test
    void consultarEstudiantePorId_devuelve_datos_personales_y_contextos_academicos_separados() {
        final NamedParameterJdbcOperations jdbcOperations = mock(NamedParameterJdbcOperations.class);
        final AtomicReference<String> detalleSql = new AtomicReference<>();
        final AtomicReference<String> contextoSql = new AtomicReference<>();
        when(jdbcOperations.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    final String sql = invocation.getArgument(0);
                    final RowMapper<?> rowMapper = invocation.getArgument(2);
                    if (sql.contains("SELECT DISTINCT")) {
                        contextoSql.set(sql);
                        return List.of(rowMapper.mapRow(contextoResultSet(), 0));
                    }
                    detalleSql.set(sql);
                    return List.of(rowMapper.mapRow(resumenResultSet(), 0));
                });
        final EstudianteRepositorySqlServerAdapter adapter = new EstudianteRepositorySqlServerAdapter(jdbcOperations);

        final Optional<EstudianteDetalleRepositoryEntity> detalle = adapter.consultarEstudiantePorId(ESTUDIANTE);

        assertTrue(detalle.isPresent());
        assertEquals(ESTUDIANTE, detalle.get().datosPersonales().id());
        assertEquals(1, detalle.get().contextosAcademicos().size());
        assertTrue(detalleSql.get().contains("FROM dbo.uv_estudiante_identidad e"));
        assertTrue(contextoSql.get().contains("SELECT DISTINCT"));
        assertTrue(contextoSql.get().contains("FROM dbo.uv_estudiante"));
    }

    @Test
    void consultarEstudiantePorId_retorna_empty_cuando_no_existe() {
        final NamedParameterJdbcOperations jdbcOperations = mock(NamedParameterJdbcOperations.class);
        when(jdbcOperations.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of());
        final EstudianteRepositorySqlServerAdapter adapter = new EstudianteRepositorySqlServerAdapter(jdbcOperations);

        assertTrue(adapter.consultarEstudiantePorId(ESTUDIANTE).isEmpty());
    }

    @Test
    void errores_dataAccess_se_convierten_en_databaseOperationException() {
        final NamedParameterJdbcOperations jdbcOperations = mock(NamedParameterJdbcOperations.class);
        when(jdbcOperations.queryForObject(anyString(), any(SqlParameterSource.class), eq(Number.class)))
                .thenThrow(new DataAccessResourceFailureException("fallo"));
        when(jdbcOperations.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenThrow(new DataAccessResourceFailureException("fallo"));
        final EstudianteRepositorySqlServerAdapter adapter = new EstudianteRepositorySqlServerAdapter(jdbcOperations);

        assertThrows(DatabaseOperationException.class, () -> adapter.consultarEstudiantes(new ConsultarEstudiantesRepositoryDTO(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                20
        )));
        assertThrows(DatabaseOperationException.class, () -> adapter.consultarEstudiantePorId(ESTUDIANTE));
    }

    @Test
    void mapRows_convierte_tipos_sql_server() throws SQLException {
        final EstudianteResumenRepositoryEntity resumen =
                EstudianteRepositorySqlServerAdapter.mapResumenRow(resumenResultSet(), 0);

        assertEquals(ESTUDIANTE, resumen.id());
        assertEquals(USUARIO, resumen.idUsuario());
        assertEquals(TIPO_IDENTIFICACION, resumen.tipoIdentificacionId());
        assertTrue(resumen.estaActivoUsuario());

        final var contexto = EstudianteRepositorySqlServerAdapter.mapContextoAcademicoRow(contextoResultSet(), 0);

        assertEquals(INSTITUCION, contexto.idInstitucion());
        assertEquals(GRUPO, contexto.idGrupo());
    }

    private ResultSet resumenResultSet() throws SQLException {
        final ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getObject("id")).thenReturn(ESTUDIANTE.toString());
        when(resultSet.getObject("idUsuario")).thenReturn(USUARIO);
        when(resultSet.getObject("tipoIdentificacionId")).thenReturn(TIPO_IDENTIFICACION.toString());
        when(resultSet.getObject("numeroIdentificacion")).thenReturn(123456789L);
        when(resultSet.getString("primerApellido")).thenReturn("PEREZ");
        when(resultSet.getString("segundoApellido")).thenReturn("GOMEZ");
        when(resultSet.getString("primerNombre")).thenReturn("ANA");
        when(resultSet.getString("segundoNombre")).thenReturn("MARIA");
        when(resultSet.getString("nombreCompleto")).thenReturn("ANA MARIA PEREZ GOMEZ");
        when(resultSet.getString("correo")).thenReturn("ana.perez@uco.edu.co");
        when(resultSet.getObject("estaActivoUsuario")).thenReturn(1);
        return resultSet;
    }

    private ResultSet contextoResultSet() throws SQLException {
        final ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getObject("idInstitucion")).thenReturn(INSTITUCION);
        when(resultSet.getString("nombreInstitucion")).thenReturn("UCO");
        when(resultSet.getObject("idFacultad")).thenReturn(FACULTAD.toString());
        when(resultSet.getString("nombreFacultad")).thenReturn("Ingenieria");
        when(resultSet.getObject("idPrograma")).thenReturn(PROGRAMA);
        when(resultSet.getString("nombrePrograma")).thenReturn("Sistemas");
        when(resultSet.getObject("idPlanEstudio")).thenReturn(PLAN);
        when(resultSet.getString("inpPlanEstudio")).thenReturn("2024");
        when(resultSet.getObject("idAsignatura")).thenReturn(ASIGNATURA);
        when(resultSet.getString("nombreAsignatura")).thenReturn("Backend");
        when(resultSet.getObject("idGrupo")).thenReturn(GRUPO.toString());
        when(resultSet.getString("nombreGrupo")).thenReturn("G1");
        return resultSet;
    }
}
