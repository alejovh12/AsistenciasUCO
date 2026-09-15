package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core;

import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsistenciasPorGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciaAutonomaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciasSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ResolverSolicitudRevisionAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.SolicitarRevisionAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.AsistenciaRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.JdbcValueMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.CanonicalStoredProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Adaptador SQL Server para consultas publicas de asistencias.
 */
public final class AsistenciaRepositorySqlServerAdapter implements AsistenciaRepositoryPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsistenciaRepositorySqlServerAdapter.class);

    private static final String PARAM_ID_GRUPO = "idGrupo";
    private static final String PARAM_ID_SESION = "idSesion";
    private static final String PARAM_ASISTENCIA_JSON = "asistenciaJSON";
    private static final String PARAM_ID_ESTUDIANTE = "idEstudiante";
    private static final String PARAM_CODIGO_VERIFICACION = "codigoVerificacion";
    private static final String PARAM_CATEGORIA = "categoria";
    private static final String PARAM_JUSTIFICACION = "justificacion";
    private static final String PARAM_SOPORTE_NOMBRE = "soporteNombre";
    private static final String PARAM_SOPORTE_URL = "soporteUrl";
    private static final String PARAM_ID_SOLICITUD = "idSolicitud";
    private static final String PARAM_ID_DOCENTE = "idDocente";
    private static final String PARAM_ACCION = "accion";
    private static final String PARAM_RESPUESTA_DOCENTE = "respuestaDocente";
    private static final String PARAM_ID_CORRELACION = "idCorrelacion";

    static final String SQL_REGISTRAR_ASISTENCIAS_SESION = """
            EXEC dbo.usp_registrar_asistencias_sesion
                 @idSesion = :idSesion,
                 @asistenciaJSON = :asistenciaJSON,
                 @idCorrelacion = :idCorrelacion
            """;

    static final String SQL_REGISTRAR_ASISTENCIA_AUTONOMA = """
            EXEC dbo.usp_registrar_asistencia_estudiante_autonomo
                 @idEstudiante = :idEstudiante,
                 @idSesion = :idSesion,
                 @codigoVerificacion = :codigoVerificacion,
                 @idCorrelacion = :idCorrelacion
            """;

    static final String SQL_RADICAR_SOLICITUD_REVISION = """
            EXEC dbo.usp_radicar_solicitud_revision_asistencia
                 @idEstudiante = :idEstudiante,
                 @idSesion = :idSesion,
                 @categoria = :categoria,
                 @justificacion = :justificacion,
                 @soporteNombre = :soporteNombre,
                 @soporteUrl = :soporteUrl,
                 @idCorrelacion = :idCorrelacion
            """;

    static final String SQL_RESOLVER_SOLICITUD_REVISION = """
            EXEC dbo.usp_resolver_solicitud_revision_asistencia
                 @idSolicitud = :idSolicitud,
                 @idDocente = :idDocente,
                 @accion = :accion,
                 @respuestaDocente = :respuestaDocente,
                 @idCorrelacion = :idCorrelacion
            """;

    static final String SQL_CONSULTAR_ASISTENCIAS = """
            SELECT
                da.id AS asistencia,
                eg.idEstudiante AS estudiante,
                eg.idGrupo AS grupo,
                a.idSesion AS sesion,
                da.asistio AS presente,
                '' AS observacion
            FROM dbo.uv_detalle_asistencia da
            INNER JOIN dbo.uv_asistencia a
                    ON a.id = da.idAsistencia
            INNER JOIN dbo.uv_estudiante_grupo eg
                    ON eg.id = a.idEstudianteGrupo
            WHERE eg.idGrupo = :idGrupo
              AND (:idSesion IS NULL OR a.idSesion = :idSesion)
            """;

    private final NamedParameterJdbcOperations jdbcOperations;
    private final CanonicalStoredProcedureExecutor procedureExecutor;
    private final ObjectMapper objectMapper;

    public AsistenciaRepositorySqlServerAdapter(
            final NamedParameterJdbcOperations jdbcOperations,
            final CanonicalStoredProcedureExecutor procedureExecutor
    ) {
        this.jdbcOperations = Objects.requireNonNull(jdbcOperations, "Las operaciones JDBC nombradas de asistencia son obligatorias.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El ejecutor canonico de asistencia es obligatorio.");
        this.objectMapper = JsonMapper.builder().build();
    }

    @Override
    public void registrarAsistencia(final RegistrarAsistenciaRepositoryDTO dto) {
        throw new FeatureUnavailableException(
                "El registro individual docente requiere contrato DB inequívoco para mapear idEstadoAsistencia."
        );
    }

    @Override
    public void registrarAsistenciasSesion(final RegistrarAsistenciasSesionRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para registrar asistencias por sesion es obligatorio.");
        }

        procedureExecutor.execute(
                "registrarAsistenciasSesion",
                SQL_REGISTRAR_ASISTENCIAS_SESION,
                new MapSqlParameterSource()
                        .addValue(PARAM_ID_SESION, dto.sesion())
                        .addValue(PARAM_ASISTENCIA_JSON, serializarRegistros(dto))
                        .addValue(PARAM_ID_CORRELACION, CorrelationIdContext.require())
        );
    }

    @Override
    public void registrarAsistenciaAutonoma(final RegistrarAsistenciaAutonomaRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para registrar asistencia autonoma es obligatorio.");
        }

        procedureExecutor.execute(
                "registrarAsistenciaAutonoma",
                SQL_REGISTRAR_ASISTENCIA_AUTONOMA,
                new MapSqlParameterSource()
                        .addValue(PARAM_ID_ESTUDIANTE, dto.estudiante())
                        .addValue(PARAM_ID_SESION, dto.sesion())
                        .addValue(PARAM_CODIGO_VERIFICACION, dto.codigoVerificacion())
                        .addValue(PARAM_ID_CORRELACION, CorrelationIdContext.require())
        );
    }

    @Override
    public List<AsistenciaRepositoryProjection> consultarAsistenciasPorGrupo(
            final ConsultarAsistenciasPorGrupoRepositoryDTO dto
    ) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para consultar asistencias por grupo es obligatorio.");
        }

        try {
            return jdbcOperations.query(
                    SQL_CONSULTAR_ASISTENCIAS,
                    new MapSqlParameterSource()
                            .addValue(PARAM_ID_GRUPO, dto.getGrupo())
                            .addValue(PARAM_ID_SESION, dto.getSesion()),
                    (rs, rowNum) -> new AsistenciaRepositoryProjection(
                            JdbcValueMapper.toUuid(rs.getObject("asistencia")),
                            JdbcValueMapper.toUuid(rs.getObject("estudiante")),
                            JdbcValueMapper.toUuid(rs.getObject("grupo")),
                            JdbcValueMapper.toUuid(rs.getObject("sesion")),
                            rs.getBoolean("presente"),
                            JdbcValueMapper.toString(rs.getObject("observacion"))
                    )
            );
        } catch (DataAccessException exception) {
            LOGGER.error(
                    "SQL operation failed. operation=consultarAsistenciasPorGrupo, correlationId={}",
                    CorrelationIdContext.getAsString()
            );
            throw new DatabaseOperationException("No fue posible consultar las asistencias de base de datos.", exception);
        }
    }

    @Override
    public void solicitarRevisionAsistencia(final SolicitarRevisionAsistenciaRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para solicitar revision de asistencia es obligatorio.");
        }

        procedureExecutor.execute(
                "solicitarRevisionAsistencia",
                SQL_RADICAR_SOLICITUD_REVISION,
                new MapSqlParameterSource()
                        .addValue(PARAM_ID_ESTUDIANTE, dto.estudiante())
                        .addValue(PARAM_ID_SESION, dto.sesion())
                        .addValue(PARAM_CATEGORIA, dto.categoria())
                        .addValue(PARAM_JUSTIFICACION, dto.justificacion())
                        .addValue(PARAM_SOPORTE_NOMBRE, dto.soporteNombre())
                        .addValue(PARAM_SOPORTE_URL, dto.soporteUrl())
                        .addValue(PARAM_ID_CORRELACION, CorrelationIdContext.require())
        );
    }

    @Override
    public void resolverSolicitudRevisionAsistencia(final ResolverSolicitudRevisionAsistenciaRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para resolver revision de asistencia es obligatorio.");
        }

        procedureExecutor.execute(
                "resolverSolicitudRevisionAsistencia",
                SQL_RESOLVER_SOLICITUD_REVISION,
                new MapSqlParameterSource()
                        .addValue(PARAM_ID_SOLICITUD, dto.solicitud())
                        .addValue(PARAM_ID_DOCENTE, dto.docente())
                        .addValue(PARAM_ACCION, dto.accion())
                        .addValue(PARAM_RESPUESTA_DOCENTE, dto.respuestaDocente())
                        .addValue(PARAM_ID_CORRELACION, CorrelationIdContext.require())
        );
    }

    private String serializarRegistros(final RegistrarAsistenciasSesionRepositoryDTO dto) {
        try {
            final List<Map<String, String>> registros = dto.registros().stream()
                    .map(registro -> Map.of(
                            "idEstudiante", registro.estudiante().toString(),
                            "estado", registro.estado()
                    ))
                    .toList();
            return objectMapper.writeValueAsString(registros);
        } catch (Exception exception) {
            throw new CrosscuttingException("No fue posible serializar los registros de asistencia.", exception);
        }
    }
}
