package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.contract;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Gate del Golden Path: valida UNICAMENTE los stored procedures y vistas consumidos por los endpoints
 * del Golden Path (GET docente/horarios, GET sesiones/grupo/{id}, GET grupos/{id}/estudiantes,
 * GET grupos/{id}/asistencias, POST asistencias/lote, creacion/actualizacion de sesion y el evento
 * realtime que el registro de asistencias publica). No sustituye a {@link SqlStoredProcedureContractIT},
 * que sigue siendo el gate GLOBAL de los 19 procedimientos y evidencia sin falsear los SP ausentes
 * fuera del Golden Path.
 *
 * <p>El realtime no tiene consulta SQL propia: se emite tras {@code usp_registrar_asistencias_sesion}
 * y proyecta desde los mismos datos, por lo que queda cubierto por ese procedimiento y por las vistas
 * de asistencias/sesion validadas aqui.</p>
 *
 * <p>Las columnas de vistas se derivan de los SELECT reales de los adapters Java
 * ({@code HorarioDocenteSqlServerAdapter}, {@code HorarioEstudianteSqlServerAdapter},
 * {@code SesionRepositorySqlServerAdapter}, {@code GrupoRepositorySqlServerAdapter},
 * {@code AsistenciaRepositorySqlServerAdapter}, {@code InstitutionalScopeSqlServerAdapter}).
 * {@code aula} ya no es consumida: {@code uv_horario_estudiante} y {@code uv_grupo} no deben exponerla.</p>
 */
@Tag("integration")
@SpringBootTest
@MockitoBean(types = JwtDecoder.class)
class GoldenPathSqlStoredProcedureContractIT {

    private static final String SQL_PARAMETROS_POR_PROCEDIMIENTO = """
            SELECT
                p.name AS parameterName,
                TYPE_NAME(p.user_type_id) AS typeName,
                p.is_output AS output
            FROM sys.objects o
            INNER JOIN sys.parameters p ON p.object_id = o.object_id
            WHERE SCHEMA_NAME(o.schema_id) = 'dbo'
              AND o.type = 'P'
              AND o.name = ?
            ORDER BY p.parameter_id
            """;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @ParameterizedTest(name = "{0}")
    @MethodSource("procedimientosGoldenPath")
    void stored_procedure_del_golden_path_coincide_parametro_a_parametro(
            final String procedureName,
            final List<SqlParameterContract> expectedParameters
    ) {
        final List<SqlParameterContract> actualParameters = jdbcTemplate.query(
                SQL_PARAMETROS_POR_PROCEDIMIENTO,
                (resultSet, rowNum) -> new SqlParameterContract(
                        resultSet.getString("parameterName"),
                        resultSet.getString("typeName"),
                        resultSet.getBoolean("output")
                ),
                procedureName
        );

        assertTrue(!actualParameters.isEmpty(),
                () -> "Contrato Golden Path incompatible: " + procedureName + " no existe en la DB.");
        assertEquals(expectedParameters, actualParameters,
                () -> "Contrato Golden Path incompatible: parametros de " + procedureName + " no coinciden con el backend.");
        assertFalse(actualParameters.stream().anyMatch(p -> "@idDocente".equalsIgnoreCase(p.parameterName())),
                () -> procedureName + " no debe declarar @idDocente: la titularidad se deriva de @idUsuarioEjecutor.");
        assertTrue(actualParameters.stream().anyMatch(p -> "@idUsuarioEjecutor".equals(p.parameterName())),
                () -> procedureName + " debe declarar @idUsuarioEjecutor.");
    }

    /**
     * {@code sys.parameters.has_default_value} no distingue "sin default" de "default = NULL"; por eso la
     * nulabilidad de {@code @idUsuarioEjecutor} se verifica contra el texto fuente ({@code OBJECT_DEFINITION}).
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("nombresProcedimientosConEjecutor")
    void idUsuarioEjecutor_permite_null_segun_texto_fuente_real(final String procedureName) {
        final String definition = jdbcTemplate.queryForObject(
                "SELECT OBJECT_DEFINITION(OBJECT_ID('dbo." + procedureName + "'))",
                String.class
        );

        assertTrue(definition != null && definition.replaceAll("\\s+", " ")
                        .contains("@idUsuarioEjecutor UNIQUEIDENTIFIER = NULL"),
                () -> "El contrato fuente de " + procedureName
                        + " cambio: @idUsuarioEjecutor ya no declara '= NULL' como valor por defecto.");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("vistasGoldenPath")
    void vista_del_golden_path_expone_las_columnas_realmente_consumidas_y_no_columnas_fantasma(
            final String viewName,
            final Set<String> columnasConsumidas,
            final Set<String> columnasProhibidas
    ) {
        final List<String> reales = jdbcTemplate.queryForList(
                "SELECT c.name FROM sys.columns c WHERE c.object_id = OBJECT_ID(?)",
                String.class,
                "dbo." + viewName
        );

        assertFalse(reales.isEmpty(), () -> "La vista dbo." + viewName + " no existe o no tiene columnas.");
        for (final String columna : columnasConsumidas) {
            assertTrue(reales.contains(columna),
                    () -> "dbo." + viewName + " debe exponer '" + columna + "' (consumida por el adapter). Reales: " + reales);
        }
        for (final String prohibida : columnasProhibidas) {
            assertFalse(reales.contains(prohibida),
                    () -> "dbo." + viewName + " no debe exponer la columna fantasma '" + prohibida + "'.");
        }

        // La vista debe resolverse ejecutable (no solo declarada): un SELECT vacio con las columnas consumidas.
        jdbcTemplate.queryForList("SELECT TOP 0 " + String.join(", ", columnasConsumidas) + " FROM dbo." + viewName);
    }

    private static Stream<String> nombresProcedimientosConEjecutor() {
        return Stream.of("usp_registrar_asistencias_sesion", "usp_crear_sesion", "usp_actualizar_sesion");
    }

    private static Stream<Arguments> procedimientosGoldenPath() {
        return Stream.of(
                Arguments.of("usp_registrar_asistencias_sesion", List.of(
                        input("@idSesion", "uniqueidentifier"),
                        input("@asistenciaJSON", "nvarchar"),
                        input("@idCorrelacion", "uniqueidentifier"),
                        input("@idUsuarioEjecutor", "uniqueidentifier")
                )),
                Arguments.of("usp_crear_sesion", List.of(
                        input("@idGrupo", "uniqueidentifier"),
                        input("@nombre", "nvarchar"),
                        input("@fechaHoraInicio", "datetime2"),
                        input("@fechaHoraFin", "datetime2"),
                        input("@idCorrelacion", "uniqueidentifier"),
                        input("@idUsuarioEjecutor", "uniqueidentifier")
                )),
                Arguments.of("usp_actualizar_sesion", List.of(
                        input("@idSesion", "uniqueidentifier"),
                        input("@nombre", "nvarchar"),
                        input("@fechaHoraInicio", "datetime2"),
                        input("@fechaHoraFin", "datetime2"),
                        input("@idCorrelacion", "uniqueidentifier"),
                        input("@idUsuarioEjecutor", "uniqueidentifier")
                ))
        );
    }

    private static Stream<Arguments> vistasGoldenPath() {
        return Stream.of(
                // GET docente/horarios
                Arguments.of("uv_horario_docente", Set.of("id", "idDocente", "idGrupo", "codigoMateria", "nombreMateria",
                        "seccion", "dia", "horaInicio", "horaFin", "totalEstudiantes"), Set.of("aula")),
                // horario estudiante: sin aula
                Arguments.of("uv_horario_estudiante", Set.of("id", "idEstudiante", "idGrupo", "codigoMateria", "nombreMateria",
                        "grupo", "dia", "horaInicio", "horaFin", "docente"), Set.of("aula")),
                // GET sesiones/grupo/{id} y consulta por id
                Arguments.of("uv_sesion", Set.of("id", "idGrupo", "nombre", "numero", "codigo", "numeroSemana",
                        "codigoGrupo", "nombreGrupo", "fechaHoraInicio", "fechaHoraFin"), Set.of("aula")),
                // Grupo sin aula (consulta de grupos y titularidad de sesion/grupo)
                Arguments.of("uv_grupo", Set.of("id", "codigo", "nombre", "idAsignatura", "nombreAsignatura", "idDocente",
                        "capacidadMaximaPermitida", "estudiantesActivos", "cuposDisponibles", "grupoEstaHablitado",
                        "fechaInicioPeriodoAcademico", "fechaFinPeriodoAcademico"), Set.of("aula")),
                // GET grupos/{id}/estudiantes y GET grupos/{id}/asistencias
                Arguments.of("uv_estudiante_grupo", Set.of("id", "idEstudiante", "idGrupo", "codigoEstadoEstudiante",
                        "nombreEstadoEstudiante"), Set.of()),
                Arguments.of("uv_estudiante_identidad", Set.of("id", "idUsuario", "numeroIdentificacion", "nombreCompleto"), Set.of()),
                Arguments.of("uv_usuario", Set.of("id", "correo"), Set.of()),
                Arguments.of("uv_asistencia", Set.of("id", "idSesion", "idEstudianteGrupo"), Set.of()),
                Arguments.of("uv_detalle_asistencia", Set.of("id", "idAsistencia", "asistio", "codigoRazonCausa"), Set.of()),
                // Ownership docente (InstitutionalScope) para los endpoints anteriores
                Arguments.of("uv_docente_identidad", Set.of("id", "idUsuario"), Set.of())
        );
    }

    private static SqlParameterContract input(final String parameterName, final String typeName) {
        return new SqlParameterContract(parameterName, typeName, false);
    }

    private record SqlParameterContract(String parameterName, String typeName, boolean output) {
    }
}
