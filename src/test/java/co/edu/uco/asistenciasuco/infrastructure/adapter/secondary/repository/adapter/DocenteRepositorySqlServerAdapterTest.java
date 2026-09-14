package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.adapter;


import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.AsignarDocenteAGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsignacionesAcademicasDocenteRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarDocentePorIdRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarDocenteDesdeUsuarioRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.DocenteAsignacionAcademicaRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.DocenteIdentidadRepositoryProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.mapper.DocenteAsignacionAcademicaRepositoryRowMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.mapper.DocenteIdentidadRepositoryRowMapper;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocenteRepositorySqlServerAdapterTest {

    private static final UUID DOCENTE = UUID.fromString("13641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID USUARIO = UUID.fromString("23641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID GRUPO = UUID.fromString("33641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID CORRELACION = UUID.fromString("93641bab-e3cd-485c-b275-47e7b731e18c");

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
                (sql, args) -> List.of()
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
                (sql, args) -> List.of()
        );

        final Optional<DocenteIdentidadRepositoryProjection> resultado =
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
                }
        );

        final List<DocenteAsignacionAcademicaRepositoryProjection> resultado =
                adapter.consultarAsignacionesAcademicas(
                        new ConsultarAsignacionesAcademicasDocenteRepositoryDTO(DOCENTE)
                );

        assertEquals(2, resultado.size());
        assertTrue(sqlCapturado.get().contains("FROM dbo.uv_docente"));
        assertTrue(sqlCapturado.get().contains("ORDER BY"));
        assertTrue(sqlCapturado.get().contains("idGrupo"));
    }

    @Test
    void comandos_docente_no_soportados_lanzan_featureUnavailable_sin_infraestructura_ficticia() {
        final DocenteRepositorySqlServerAdapter adapter = adapterBase(
                sql -> List.of(),
                (sql, args) -> List.of(),
                (sql, args) -> List.of()
        );

        assertThrows(
                FeatureUnavailableException.class,
                () -> adapter.registrarDocenteDesdeUsuario(new RegistrarDocenteDesdeUsuarioRepositoryDTO(USUARIO))
        );
        assertThrows(
                FeatureUnavailableException.class,
                () -> adapter.asignarDocenteAGrupo(new AsignarDocenteAGrupoRepositoryDTO(DOCENTE, GRUPO))
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
    }

    @Test
    void mapRows_mapean_todas_las_columnas() throws SQLException {
        final ResultSet identidad = mock(ResultSet.class);
        when(identidad.getObject("id")).thenReturn(DOCENTE);
        when(identidad.getObject("idUsuario")).thenReturn(USUARIO.toString());
        when(identidad.getObject("numeroIdentificacion")).thenReturn(123456789);
        when(identidad.getString("nombreCompleto")).thenReturn("Ana Perez");
        when(identidad.getObject("estaActivoUsuario")).thenReturn(1);

        final DocenteIdentidadRepositoryProjection identidadEntity =
                new DocenteIdentidadRepositoryRowMapper().mapRow(identidad, 0);

        assertEquals(DOCENTE, identidadEntity.getId());
        assertEquals(USUARIO, identidadEntity.getIdUsuario());
        assertTrue(identidadEntity.isEstaActivoUsuario());

        final ResultSet asignacion = mockAsignacionResultSet();
        final DocenteAsignacionAcademicaRepositoryProjection asignacionEntity =
                new DocenteAsignacionAcademicaRepositoryRowMapper().mapRow(asignacion, 0);

        assertEquals("UCO", asignacionEntity.getNombreInstitucion());
        assertEquals("Backend", asignacionEntity.getNombreAsignatura());
        assertEquals(1, asignacionEntity.getEstaActivoDocente());
    }

    private DocenteRepositorySqlServerAdapter adapterBase(
            final DocenteRepositorySqlServerAdapter.QueryExecutor<DocenteIdentidadRepositoryProjection> consultarDocentes,
            final DocenteRepositorySqlServerAdapter.ParameterizedQueryExecutor<DocenteIdentidadRepositoryProjection> consultarPorId,
            final DocenteRepositorySqlServerAdapter.ParameterizedQueryExecutor<DocenteAsignacionAcademicaRepositoryProjection> consultarAsignaciones
    ) {
        return new DocenteRepositorySqlServerAdapter(
                consultarDocentes,
                consultarPorId,
                consultarAsignaciones
        );
    }

    private DocenteAsignacionAcademicaRepositoryProjection asignacion(final Integer estadoDocente) {
        return new DocenteAsignacionAcademicaRepositoryProjection(
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
