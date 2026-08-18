package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository;

import co.edu.uco.asistenciasuco.application.exception.DatabaseOperationException;
import co.edu.uco.asistenciasuco.application.secondaryports.DocenteRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.AsignarDocenteAGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsignacionesAcademicasDocenteRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarDocentePorIdRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarDocenteDesdeUsuarioRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.DocenteAsignacionAcademicaRepositoryEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.DocenteIdentidadRepositoryEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.DocenteOperacionRepositoryEntity;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.error.DbExceptionTranslator;
import co.edu.uco.asistenciasuco.infrastructure.correlation.CorrelationIdContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador SQL Server para operaciones confirmadas de docentes.
 */
public final class DocenteRepositorySqlServerAdapter implements DocenteRepositoryPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocenteRepositorySqlServerAdapter.class);
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

    private final QueryExecutor<DocenteIdentidadRepositoryEntity> consultarDocentesExecutor;
    private final ParameterizedQueryExecutor<DocenteIdentidadRepositoryEntity> consultarDocentePorIdExecutor;
    private final ParameterizedQueryExecutor<DocenteAsignacionAcademicaRepositoryEntity> consultarAsignacionesExecutor;
    private final ProcedureExecutor registrarDocenteExecutor;
    private final ProcedureExecutor asignarDocenteExecutor;

    public DocenteRepositorySqlServerAdapter(final JdbcTemplate jdbcTemplate) {
        if (ObjectHelper.isNull(jdbcTemplate)) {
            throw new CrosscuttingException("El JdbcTemplate para docentes es obligatorio.");
        }

        this.consultarDocentesExecutor = sql -> jdbcTemplate.query(sql, DocenteRepositorySqlServerAdapter::mapIdentidadRow);
        this.consultarDocentePorIdExecutor =
                (sql, args) -> jdbcTemplate.query(sql, DocenteRepositorySqlServerAdapter::mapIdentidadRow, args);
        this.consultarAsignacionesExecutor =
                (sql, args) -> jdbcTemplate.query(sql, DocenteRepositorySqlServerAdapter::mapAsignacionRow, args);
        this.registrarDocenteExecutor = crearRegistrarDocenteCall(jdbcTemplate)::execute;
        this.asignarDocenteExecutor = crearAsignarDocenteCall(jdbcTemplate)::execute;
    }

    DocenteRepositorySqlServerAdapter(
            final QueryExecutor<DocenteIdentidadRepositoryEntity> consultarDocentesExecutor,
            final ParameterizedQueryExecutor<DocenteIdentidadRepositoryEntity> consultarDocentePorIdExecutor,
            final ParameterizedQueryExecutor<DocenteAsignacionAcademicaRepositoryEntity> consultarAsignacionesExecutor,
            final ProcedureExecutor registrarDocenteExecutor,
            final ProcedureExecutor asignarDocenteExecutor
    ) {
        if (ObjectHelper.isNull(consultarDocentesExecutor)) {
            throw new CrosscuttingException("El ejecutor para consultar docentes es obligatorio.");
        }
        if (ObjectHelper.isNull(consultarDocentePorIdExecutor)) {
            throw new CrosscuttingException("El ejecutor para consultar docente por ID es obligatorio.");
        }
        if (ObjectHelper.isNull(consultarAsignacionesExecutor)) {
            throw new CrosscuttingException("El ejecutor para consultar asignaciones de docente es obligatorio.");
        }
        if (ObjectHelper.isNull(registrarDocenteExecutor)) {
            throw new CrosscuttingException("El ejecutor para registrar docente desde usuario es obligatorio.");
        }
        if (ObjectHelper.isNull(asignarDocenteExecutor)) {
            throw new CrosscuttingException("El ejecutor para asignar docente a grupo es obligatorio.");
        }
        this.consultarDocentesExecutor = consultarDocentesExecutor;
        this.consultarDocentePorIdExecutor = consultarDocentePorIdExecutor;
        this.consultarAsignacionesExecutor = consultarAsignacionesExecutor;
        this.registrarDocenteExecutor = registrarDocenteExecutor;
        this.asignarDocenteExecutor = asignarDocenteExecutor;
    }

    @Override
    public List<DocenteIdentidadRepositoryEntity> consultarDocentes() {
        try {
            return consultarDocentesExecutor.query(SQL_CONSULTAR_TODOS);
        } catch (DataAccessException exception) {
            LOGGER.error(
                    "SQL operation failed. operation={}, correlationId={}, exceptionType={}",
                    OPERATION_CONSULTAR_DOCENTES,
                    CorrelationIdContext.getAsString(),
                    exception.getClass().getSimpleName()
            );
            throw new DatabaseOperationException("No fue posible consultar los docentes.", exception);
        }
    }

    @Override
    public Optional<DocenteIdentidadRepositoryEntity> consultarDocentePorId(
            final ConsultarDocentePorIdRepositoryDTO dto
    ) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para consultar docente por ID es obligatorio.");
        }

        try {
            final List<DocenteIdentidadRepositoryEntity> resultado =
                    consultarDocentePorIdExecutor.query(SQL_CONSULTAR_POR_ID, dto.getDocente().toString());
            return resultado.isEmpty() ? Optional.empty() : Optional.of(resultado.get(0));
        } catch (DataAccessException exception) {
            LOGGER.error(
                    "SQL operation failed. operation={}, correlationId={}, exceptionType={}",
                    OPERATION_CONSULTAR_DOCENTE_POR_ID,
                    CorrelationIdContext.getAsString(),
                    exception.getClass().getSimpleName()
            );
            throw new DatabaseOperationException("No fue posible consultar el docente por ID.", exception);
        }
    }

    @Override
    public List<DocenteAsignacionAcademicaRepositoryEntity> consultarAsignacionesAcademicas(
            final ConsultarAsignacionesAcademicasDocenteRepositoryDTO dto
    ) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para consultar asignaciones academicas del docente es obligatorio.");
        }

        try {
            return consultarAsignacionesExecutor.query(SQL_CONSULTAR_ASIGNACIONES, dto.getDocente().toString());
        } catch (DataAccessException exception) {
            LOGGER.error(
                    "SQL operation failed. operation={}, correlationId={}, exceptionType={}",
                    OPERATION_CONSULTAR_ASIGNACIONES,
                    CorrelationIdContext.getAsString(),
                    exception.getClass().getSimpleName()
            );
            throw new DatabaseOperationException(
                    "No fue posible consultar las asignaciones academicas del docente.",
                    exception
            );
        }
    }

    @Override
    public DocenteOperacionRepositoryEntity registrarDocenteDesdeUsuario(
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
            return toOperacionEntity(result, correlationId, OPERATION_REGISTRAR_DOCENTE);
        } catch (DataAccessException exception) {
            LOGGER.error(
                    "SQL operation failed. operation={}, correlationId={}, exceptionType={}",
                    OPERATION_REGISTRAR_DOCENTE,
                    CorrelationIdContext.getAsString(),
                    exception.getClass().getSimpleName()
            );
            throw new DatabaseOperationException(
                    "No fue posible ejecutar el procedimiento de registro de docente desde usuario.",
                    exception
            );
        }
    }

    @Override
    public DocenteOperacionRepositoryEntity asignarDocenteAGrupo(final AsignarDocenteAGrupoRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para asignar docente a grupo es obligatorio.");
        }

        try {
            final UUID correlationId = CorrelationIdContext.require();
            final Map<String, Object> result = asignarDocenteExecutor.execute(buildAsignarDocenteParameters(dto, correlationId));
            return toOperacionEntity(result, correlationId, OPERATION_ASIGNAR_DOCENTE);
        } catch (DataAccessException exception) {
            LOGGER.error(
                    "SQL operation failed. operation={}, correlationId={}, exceptionType={}",
                    OPERATION_ASIGNAR_DOCENTE,
                    CorrelationIdContext.getAsString(),
                    exception.getClass().getSimpleName()
            );
            throw new DatabaseOperationException(
                    "No fue posible ejecutar el procedimiento de asignacion de docente a grupo.",
                    exception
            );
        }
    }

    static DocenteIdentidadRepositoryEntity mapIdentidadRow(final ResultSet resultSet, final int rowNumber)
            throws SQLException {
        return new DocenteIdentidadRepositoryEntity(
                toUuid(resultSet.getObject("id")),
                toUuid(resultSet.getObject("idUsuario")),
                toInteger(resultSet.getObject("numeroIdentificacion")),
                resultSet.getString("nombreCompleto"),
                toBoolean(resultSet.getObject("estaActivoUsuario"))
        );
    }

    static DocenteAsignacionAcademicaRepositoryEntity mapAsignacionRow(final ResultSet resultSet, final int rowNumber)
            throws SQLException {
        return new DocenteAsignacionAcademicaRepositoryEntity(
                toUuid(resultSet.getObject("id")),
                toUuid(resultSet.getObject("idUsuario")),
                toInteger(resultSet.getObject("numeroIdentificacion")),
                resultSet.getString("nombreCompleto"),
                toBoolean(resultSet.getObject("estaActivoUsuario")),
                toUuid(resultSet.getObject("idInstitucion")),
                resultSet.getString("nombreInstitucion"),
                toUuid(resultSet.getObject("idFacultad")),
                resultSet.getString("nombreFacultad"),
                toUuid(resultSet.getObject("idPrograma")),
                resultSet.getString("nombrePrograma"),
                toUuid(resultSet.getObject("idPlanEstudio")),
                resultSet.getString("inpPlanEstudio"),
                toUuid(resultSet.getObject("idAsignatura")),
                resultSet.getString("nombreAsignatura"),
                toUuid(resultSet.getObject("idGrupo")),
                resultSet.getString("nombreGrupo"),
                toUuid(resultSet.getObject("idPerfil")),
                resultSet.getString("codigoPerfil"),
                resultSet.getString("nombrePerfil"),
                toInteger(resultSet.getObject("estaActivoDocente")),
                resultSet.getString("estaActivoTextoDocente")
        );
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

    private DocenteOperacionRepositoryEntity toOperacionEntity(
            final Map<String, Object> result,
            final UUID correlationId,
            final String operation
    ) {
        DbExceptionTranslator.throwIfFailed(
                toBoolean(result.get(OUT_ESTADO)),
                toString(result.get(OUT_MENSAJE_USUARIO)),
                toString(result.get(OUT_MENSAJE_TECNICO)),
                correlationId.toString(),
                operation
        );
        return new DocenteOperacionRepositoryEntity(toString(result.get(OUT_MENSAJE_USUARIO)));
    }

    private static UUID toUuid(final Object value) {
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return ObjectHelper.isNull(value) ? null : UUID.fromString(String.valueOf(value));
    }

    private static Integer toInteger(final Object value) {
        if (ObjectHelper.isNull(value)) {
            return null;
        }
        if (value instanceof Number numberValue) {
            return numberValue.intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }

    private static boolean toBoolean(final Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof Number numberValue) {
            return numberValue.intValue() == 1;
        }
        if (value instanceof String stringValue) {
            return "true".equalsIgnoreCase(stringValue) || "1".equals(stringValue);
        }
        return false;
    }

    private static String toString(final Object value) {
        return ObjectHelper.isNull(value) ? null : String.valueOf(value);
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
