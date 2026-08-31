package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.adapter;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.DocenteRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.AsignarDocenteAGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsignacionesAcademicasDocenteRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarDocentePorIdRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarDocenteDesdeUsuarioRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.DocenteAsignacionAcademicaRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.DocenteIdentidadRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.DocenteOperacionRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.error.DbExceptionTranslator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.mapper.DocenteAsignacionAcademicaRepositoryRowMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.mapper.DocenteIdentidadRepositoryRowMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.mapper.JdbcValueMapper;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;

import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Objects;

/**
 * Adaptador SQL Server para operaciones confirmadas de docentes.
 */
public final class DocenteRepositorySqlServerAdapter implements DocenteRepositoryPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocenteRepositorySqlServerAdapter.class);
    private static final RowMapper<DocenteIdentidadRepositoryProjection> DOCENTE_IDENTIDAD_ROW_MAPPER =
            new DocenteIdentidadRepositoryRowMapper();
    private static final RowMapper<DocenteAsignacionAcademicaRepositoryProjection> DOCENTE_ASIGNACION_ROW_MAPPER =
            new DocenteAsignacionAcademicaRepositoryRowMapper();
    private static final String OPERATION_CONSULTAR_DOCENTES = "consultarDocentes";
    private static final String OPERATION_CONSULTAR_DOCENTE_POR_ID = "consultarDocentePorId";
    private static final String OPERATION_CONSULTAR_ASIGNACIONES = "consultarAsignacionesAcademicas";
    private static final String OPERATION_REGISTRAR_DOCENTE = "registrarDocenteDesdeUsuario";
    private static final String OPERATION_ASIGNAR_DOCENTE = "asignarDocenteAGrupo";

    static final String SQL_CONSULTAR_TODOS = """
            SELECT
                id,
                idUsuario,
                numeroIdentificacion,
                nombreCompleto,
                estaActivoUsuario
            FROM dbo.uv_docente_identidad
            ORDER BY nombreCompleto, id
            """;

    static final String SQL_CONSULTAR_POR_ID = """
            SELECT
                id,
                idUsuario,
                numeroIdentificacion,
                nombreCompleto,
                estaActivoUsuario
            FROM dbo.uv_docente_identidad
            WHERE id = ?
            """;
    static final String SQL_CONSULTAR_POR_USUARIO = """
            SELECT
                id,
                idUsuario,
                numeroIdentificacion,
                nombreCompleto,
                estaActivoUsuario
            FROM dbo.uv_docente_identidad
            WHERE idUsuario = ?
            ORDER BY id
            """;

    static final String SQL_CONSULTAR_ASIGNACIONES = """
            SELECT
                id,
                idUsuario,
                numeroIdentificacion,
                nombreCompleto,
                estaActivoUsuario,
                idInstitucion,
                nombreInstitucion,
                idFacultad,
                nombreFacultad,
                idPrograma,
                nombrePrograma,
                idPlanEstudio,
                inpPlanEstudio,
                idAsignatura,
                nombreAsignatura,
                idGrupo,
                nombreGrupo,
                idPerfil,
                codigoPerfil,
                nombrePerfil,
                estaActivoDocente,
                estaActivoTextoDocente
            FROM dbo.uv_docente
            WHERE id = ?
            ORDER BY
                nombreInstitucion,
                nombreFacultad,
                nombrePrograma,
                nombreAsignatura,
                nombreGrupo,
                idGrupo
            """;

    static final String SCHEMA_NAME = "dbo";
    static final String PROCEDURE_REGISTRAR_DOCENTE = "usp_sincronizar_docente_interno";
    static final String PROCEDURE_ASIGNAR_DOCENTE = "usp_registrar_docente_en_grupo_interno";
    static final String PARAM_ID_USUARIO = "idUsuario";
    static final String PARAM_ID_DOCENTE = "idDocente";
    static final String PARAM_ID_GRUPO = "idGrupo";
    static final String PARAM_ID_CORRELACION = "idCorrelacion";
    static final String OUT_MENSAJE_USUARIO = "mensajeUsuarioResultado";
    static final String OUT_MENSAJE_TECNICO = "mensajeTecnicoResultado";
    static final String OUT_ESTADO = "estadoResultado";

    private final QueryExecutor<DocenteIdentidadRepositoryProjection> consultarDocentesExecutor;
    private final ParameterizedQueryExecutor<DocenteIdentidadRepositoryProjection> consultarDocentePorIdExecutor;
    private final ParameterizedQueryExecutor<DocenteAsignacionAcademicaRepositoryProjection> consultarAsignacionesExecutor;
    private final ProcedureExecutor registrarDocenteExecutor;
    private final ProcedureExecutor asignarDocenteExecutor;

    public DocenteRepositorySqlServerAdapter(final JdbcTemplate jdbcTemplate) {
        Objects.requireNonNull(jdbcTemplate, "El JdbcTemplate para docentes es obligatorio.");

        this.consultarDocentesExecutor = sql -> jdbcTemplate.query(sql, DOCENTE_IDENTIDAD_ROW_MAPPER);
        this.consultarDocentePorIdExecutor =
                (sql, args) -> jdbcTemplate.query(sql, DOCENTE_IDENTIDAD_ROW_MAPPER, args);
        this.consultarAsignacionesExecutor =
                (sql, args) -> jdbcTemplate.query(sql, DOCENTE_ASIGNACION_ROW_MAPPER, args);
        this.registrarDocenteExecutor = crearRegistrarDocenteCall(jdbcTemplate)::execute;
        this.asignarDocenteExecutor = crearAsignarDocenteCall(jdbcTemplate)::execute;
    }

    DocenteRepositorySqlServerAdapter(
            final QueryExecutor<DocenteIdentidadRepositoryProjection> consultarDocentesExecutor,
            final ParameterizedQueryExecutor<DocenteIdentidadRepositoryProjection> consultarDocentePorIdExecutor,
            final ParameterizedQueryExecutor<DocenteAsignacionAcademicaRepositoryProjection> consultarAsignacionesExecutor,
            final ProcedureExecutor registrarDocenteExecutor,
            final ProcedureExecutor asignarDocenteExecutor
    ) {
        this.consultarDocentesExecutor = Objects.requireNonNull(
                consultarDocentesExecutor,
                "El ejecutor para consultar docentes es obligatorio."
        );
        this.consultarDocentePorIdExecutor = Objects.requireNonNull(
                consultarDocentePorIdExecutor,
                "El ejecutor para consultar docente por ID es obligatorio."
        );
        this.consultarAsignacionesExecutor = Objects.requireNonNull(
                consultarAsignacionesExecutor,
                "El ejecutor para consultar asignaciones de docente es obligatorio."
        );
        this.registrarDocenteExecutor = Objects.requireNonNull(
                registrarDocenteExecutor,
                "El ejecutor para registrar docente desde usuario es obligatorio."
        );
        this.asignarDocenteExecutor = Objects.requireNonNull(
                asignarDocenteExecutor,
                "El ejecutor para asignar docente a grupo es obligatorio."
        );
    }

    @Override
    public List<DocenteIdentidadRepositoryProjection> consultarDocentes() {
        try {
            return consultarDocentesExecutor.query(SQL_CONSULTAR_TODOS);
        } catch (DataAccessException exception) {
            LOGGER.error(
                    "SQL operation failed. operation={}, correlationId={}",
                    OPERATION_CONSULTAR_DOCENTES,
                    CorrelationIdContext.getAsString()
            );
            throw new DatabaseOperationException("No fue posible consultar los docentes.", exception);
        }
    }

    @Override
    public Optional<DocenteIdentidadRepositoryProjection> consultarDocentePorId(
            final ConsultarDocentePorIdRepositoryDTO dto
    ) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para consultar docente por ID es obligatorio.");
        }

        try {
            final List<DocenteIdentidadRepositoryProjection> resultado =
                    consultarDocentePorIdExecutor.query(SQL_CONSULTAR_POR_ID, dto.getDocente().toString());
            return resultado.isEmpty() ? Optional.empty() : Optional.of(resultado.get(0));
        } catch (DataAccessException exception) {
            LOGGER.error(
                    "SQL operation failed. operation={}, correlationId={}",
                    OPERATION_CONSULTAR_DOCENTE_POR_ID,
                    CorrelationIdContext.getAsString()
            );
            throw new DatabaseOperationException("No fue posible consultar el docente por ID.", exception);
        }
    }

    @Override
    public List<DocenteAsignacionAcademicaRepositoryProjection> consultarAsignacionesAcademicas(
            final ConsultarAsignacionesAcademicasDocenteRepositoryDTO dto
    ) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para consultar asignaciones academicas del docente es obligatorio.");
        }

        try {
            return consultarAsignacionesExecutor.query(SQL_CONSULTAR_ASIGNACIONES, dto.getDocente().toString());
        } catch (DataAccessException exception) {
            LOGGER.error(
                    "SQL operation failed. operation={}, correlationId={}",
                    OPERATION_CONSULTAR_ASIGNACIONES,
                    CorrelationIdContext.getAsString()
            );
            throw new DatabaseOperationException(
                    "No fue posible consultar las asignaciones academicas del docente.",
                    exception
            );
        }
    }

    @Override
    public DocenteOperacionRepositoryProjection registrarDocenteDesdeUsuario(
            final RegistrarDocenteDesdeUsuarioRepositoryDTO dto
    ) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para registrar docente desde usuario es obligatorio.");
        }

        try {
            final UUID correlationId = CorrelationIdContext.require();
            final Map<String, Object> result = registrarDocenteExecutor.execute(
                    buildRegistrarDocenteParameters(dto, correlationId)
            );
            return toOperacionEntity(result, correlationId, OPERATION_REGISTRAR_DOCENTE, dto.getUsuario());
        } catch (DataAccessException exception) {
            LOGGER.error(
                    "SQL operation failed. operation={}, correlationId={}",
                    OPERATION_REGISTRAR_DOCENTE,
                    CorrelationIdContext.getAsString()
            );
            throw new DatabaseOperationException(
                    "No fue posible ejecutar el procedimiento de registro de docente desde usuario.",
                    exception
            );
        }
    }

    @Override
    public DocenteOperacionRepositoryProjection asignarDocenteAGrupo(final AsignarDocenteAGrupoRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para asignar docente a grupo es obligatorio.");
        }

        try {
            final UUID correlationId = CorrelationIdContext.require();
            final Map<String, Object> result = asignarDocenteExecutor.execute(buildAsignarDocenteParameters(dto, correlationId));
            return toOperacionEntity(result, correlationId, OPERATION_ASIGNAR_DOCENTE, null);
        } catch (DataAccessException exception) {
            LOGGER.error(
                    "SQL operation failed. operation={}, correlationId={}",
                    OPERATION_ASIGNAR_DOCENTE,
                    CorrelationIdContext.getAsString()
            );
            throw new DatabaseOperationException(
                    "No fue posible ejecutar el procedimiento de asignacion de docente a grupo.",
                    exception
            );
        }
    }

    private static SimpleJdbcCall crearRegistrarDocenteCall(final JdbcTemplate jdbcTemplate) {
        return new SimpleJdbcCall(jdbcTemplate)
                .withSchemaName(SCHEMA_NAME)
                .withProcedureName(PROCEDURE_REGISTRAR_DOCENTE)
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter(PARAM_ID_USUARIO, Types.VARCHAR),
                        new SqlParameter(PARAM_ID_CORRELACION, Types.VARCHAR),
                        new SqlOutParameter(OUT_MENSAJE_USUARIO, Types.NVARCHAR),
                        new SqlOutParameter(OUT_MENSAJE_TECNICO, Types.NVARCHAR),
                        new SqlOutParameter(OUT_ESTADO, Types.BIT)
                );
    }

    private static SimpleJdbcCall crearAsignarDocenteCall(final JdbcTemplate jdbcTemplate) {
        return new SimpleJdbcCall(jdbcTemplate)
                .withSchemaName(SCHEMA_NAME)
                .withProcedureName(PROCEDURE_ASIGNAR_DOCENTE)
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter(PARAM_ID_DOCENTE, Types.VARCHAR),
                        new SqlParameter(PARAM_ID_GRUPO, Types.VARCHAR),
                        new SqlParameter(PARAM_ID_CORRELACION, Types.VARCHAR),
                        new SqlOutParameter(OUT_MENSAJE_USUARIO, Types.NVARCHAR),
                        new SqlOutParameter(OUT_MENSAJE_TECNICO, Types.NVARCHAR),
                        new SqlOutParameter(OUT_ESTADO, Types.BIT)
                );
    }

    private MapSqlParameterSource buildRegistrarDocenteParameters(
            final RegistrarDocenteDesdeUsuarioRepositoryDTO dto,
            final UUID correlationId
    ) {
        return new MapSqlParameterSource()
                .addValue(PARAM_ID_USUARIO, dto.getUsuario().toString())
                .addValue(PARAM_ID_CORRELACION, correlationId.toString());
    }

    private MapSqlParameterSource buildAsignarDocenteParameters(
            final AsignarDocenteAGrupoRepositoryDTO dto,
            final UUID correlationId
    ) {
        return new MapSqlParameterSource()
                .addValue(PARAM_ID_DOCENTE, dto.getDocente().toString())
                .addValue(PARAM_ID_GRUPO, dto.getGrupo().toString())
                .addValue(PARAM_ID_CORRELACION, correlationId.toString());
    }

    private DocenteOperacionRepositoryProjection toOperacionEntity(
            final Map<String, Object> result,
            final UUID correlationId,
            final String operation,
            final UUID usuarioId
    ) {
        DbExceptionTranslator.throwIfFailed(
                JdbcValueMapper.toBoolean(result.get(OUT_ESTADO)),
                JdbcValueMapper.toString(result.get(OUT_MENSAJE_USUARIO)),
                JdbcValueMapper.toString(result.get(OUT_MENSAJE_TECNICO)),
                correlationId.toString(),
                operation
        );
        final UUID docenteId = usuarioId == null ? null : consultarDocenteIdPorUsuario(usuarioId);
        return new DocenteOperacionRepositoryProjection(
                docenteId,
                JdbcValueMapper.toString(result.get(OUT_MENSAJE_USUARIO))
        );
    }

    private UUID consultarDocenteIdPorUsuario(final UUID usuarioId) {
        try {
            final List<DocenteIdentidadRepositoryProjection> resultado =
                    consultarDocentePorIdExecutor.query(SQL_CONSULTAR_POR_USUARIO, usuarioId.toString());
            return resultado.isEmpty() ? null : resultado.get(0).getId();
        } catch (DataAccessException exception) {
            LOGGER.warn(
                    "Audit helper query failed. operation={}, correlationId={}",
                    "consultarDocenteIdPorUsuario",
                    CorrelationIdContext.getAsString()
            );
            return null;
        }
    }

    interface QueryExecutor<T> {

        List<T> query(String sql);
    }

    interface ParameterizedQueryExecutor<T> {

        List<T> query(String sql, Object... args);
    }

    interface ProcedureExecutor {

        Map<String, Object> execute(MapSqlParameterSource parameters);
    }
}
