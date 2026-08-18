package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository;

import co.edu.uco.asistenciasuco.application.exception.DatabaseOperationException;
import co.edu.uco.asistenciasuco.application.secondaryports.GrupoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarEstudianteRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.GrupoRepositoryEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.RegistrarEstudianteRepositoryEntity;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.error.DbExceptionTranslator;
import co.edu.uco.asistenciasuco.infrastructure.correlation.CorrelationIdContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Adaptador SQL Server para operaciones confirmadas de grupos.
 */
public final class GrupoRepositorySqlServerAdapter implements GrupoRepositoryPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrupoRepositorySqlServerAdapter.class);
    private static final String OPERATION_REGISTRAR_ESTUDIANTE = "registrarEstudianteEnGrupo";
    private static final String OPERATION_CONSULTAR_GRUPOS = "consultarGrupos";

    static final String PARAM_ID_TIPO_ID_IDENTIFICACION = "idTipoIdIdentificacion";
    static final String PARAM_NUMERO_IDENTIFICACION = "numeroIdentificacion";
    static final String PARAM_PRIMER_APELLIDO = "primerApellido";
    static final String PARAM_SEGUNDO_APELLIDO = "segundoApellido";
    static final String PARAM_PRIMER_NOMBRE = "primerNombre";
    static final String PARAM_SEGUNDO_NOMBRE = "segundoNombre";
    static final String PARAM_CORREO = "correo";
    static final String PARAM_PASSWORD = "password";
    static final String PARAM_ID_GRUPO = "idGrupo";
    static final String PARAM_ID_CORRELACION = "idCorrelacion";
    static final String OUT_MENSAJE_USUARIO = "mensajeUsuarioResultado";
    static final String OUT_MENSAJE_TECNICO = "mensajeTecnicoResultado";
    static final String OUT_ESTADO = "estadoResultado";

    static final String SQL_REGISTRAR_ESTUDIANTE = """
            EXEC dbo.usp_registrar_estudiante_en_grupo_usuario_no_existente
                @idTipoIdIdentificacion = :idTipoIdIdentificacion,
                @numeroIdentificacion = :numeroIdentificacion,
                @primerApellido = :primerApellido,
                @segundoApellido = :segundoApellido,
                @primerNombre = :primerNombre,
                @segundoNombre = :segundoNombre,
                @correo = :correo,
                @password = :password,
                @idGrupo = :idGrupo,
                @idCorrelacion = :idCorrelacion
            """;

    static final String SQL_CONSULTAR_GRUPOS = """
            SELECT
                id,
                codigo,
                nombre,
                idAsignatura,
                nombreAsignatura,
                idDocente,
                capacidadMaximaPermitida,
                estudiantesActivos,
                cuposDisponibles,
                grupoEstaHablitado,
                fechaInicioPeriodoAcademico,
                fechaFinPeriodoAcademico
            FROM dbo.uv_grupo
            ORDER BY nombreAsignatura, codigo, nombre, id
            """;

    private final ProcedureQueryExecutor procedureQueryExecutor;
    private final GrupoQueryExecutor grupoQueryExecutor;
    private final TransactionOperations transactionOperations;

    public GrupoRepositorySqlServerAdapter(
            final JdbcTemplate jdbcTemplate,
            final TransactionTemplate transactionTemplate
    ) {
        if (ObjectHelper.isNull(jdbcTemplate)) {
            throw new CrosscuttingException("El JdbcTemplate para grupos es obligatorio.");
        }
        if (ObjectHelper.isNull(transactionTemplate)) {
            throw new CrosscuttingException("El TransactionTemplate para grupos es obligatorio.");
        }

        final NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        this.procedureQueryExecutor = namedParameterJdbcTemplate::queryForMap;
        this.grupoQueryExecutor = (sql, rowMapper) -> jdbcTemplate.query(sql, rowMapper);
        this.transactionOperations = transactionTemplate;
    }

    GrupoRepositorySqlServerAdapter(
            final ProcedureQueryExecutor procedureQueryExecutor,
            final GrupoQueryExecutor grupoQueryExecutor,
            final TransactionOperations transactionOperations
    ) {
        if (ObjectHelper.isNull(procedureQueryExecutor)) {
            throw new CrosscuttingException("El ejecutor del procedimiento de grupos es obligatorio.");
        }
        if (ObjectHelper.isNull(grupoQueryExecutor)) {
            throw new CrosscuttingException("El ejecutor de consulta de grupos es obligatorio.");
        }
        if (ObjectHelper.isNull(transactionOperations)) {
            throw new CrosscuttingException("El manejador transaccional de grupos es obligatorio.");
        }
        this.procedureQueryExecutor = procedureQueryExecutor;
        this.grupoQueryExecutor = grupoQueryExecutor;
        this.transactionOperations = transactionOperations;
    }

    @Override
    public RegistrarEstudianteRepositoryEntity registrarEstudianteEnGrupo(final RegistrarEstudianteRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para registrar estudiante en grupo es obligatorio.");
        }

        try {
            return transactionOperations.execute(status -> ejecutarRegistro(dto));
        } catch (DataAccessException exception) {
            LOGGER.error(
                    "SQL operation failed. operation={}, correlationId={}, exceptionType={}",
                    OPERATION_REGISTRAR_ESTUDIANTE,
                    CorrelationIdContext.getAsString(),
                    exception.getClass().getSimpleName()
            );
            throw new DatabaseOperationException(
                    "No fue posible ejecutar el procedimiento de registro de estudiante en grupo.",
                    exception
            );
        }
    }

    @Override
    public List<GrupoRepositoryEntity> consultarGrupos() {
        try {
            return grupoQueryExecutor.query(SQL_CONSULTAR_GRUPOS, GrupoRepositorySqlServerAdapter::mapGrupoRow);
        } catch (DataAccessException exception) {
            LOGGER.error(
                    "SQL operation failed. operation={}, correlationId={}, exceptionType={}",
                    OPERATION_CONSULTAR_GRUPOS,
                    CorrelationIdContext.getAsString(),
                    exception.getClass().getSimpleName()
            );
            throw new DatabaseOperationException("No fue posible consultar los grupos.", exception);
        }
    }

    private RegistrarEstudianteRepositoryEntity ejecutarRegistro(final RegistrarEstudianteRepositoryDTO dto) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> result = procedureQueryExecutor.queryForMap(
                SQL_REGISTRAR_ESTUDIANTE,
                buildParameters(dto, correlationId)
        );

        DbExceptionTranslator.throwIfFailed(
                toBoolean(result.get(OUT_ESTADO)),
                toString(result.get(OUT_MENSAJE_USUARIO)),
                toString(result.get(OUT_MENSAJE_TECNICO)),
                correlationId.toString(),
                OPERATION_REGISTRAR_ESTUDIANTE
        );

        return new RegistrarEstudianteRepositoryEntity(toString(result.get(OUT_MENSAJE_USUARIO)));
    }

    private MapSqlParameterSource buildParameters(final RegistrarEstudianteRepositoryDTO dto, final UUID correlationId) {
        return new MapSqlParameterSource()
                .addValue(PARAM_ID_TIPO_ID_IDENTIFICACION, dto.getTipoIdentificacionId().toString())
                .addValue(PARAM_NUMERO_IDENTIFICACION, dto.getNumeroIdentificacion())
                .addValue(PARAM_PRIMER_APELLIDO, dto.getPrimerApellido())
                .addValue(PARAM_SEGUNDO_APELLIDO, dto.getSegundoApellido())
                .addValue(PARAM_PRIMER_NOMBRE, dto.getPrimerNombre())
                .addValue(PARAM_SEGUNDO_NOMBRE, dto.getSegundoNombre())
                .addValue(PARAM_CORREO, dto.getCorreo())
                .addValue(PARAM_PASSWORD, dto.getPassword())
                .addValue(PARAM_ID_GRUPO, dto.getGrupoId().toString())
                .addValue(PARAM_ID_CORRELACION, correlationId.toString());
    }

    static GrupoRepositoryEntity mapGrupoRow(final ResultSet resultSet, final int rowNumber) throws SQLException {
        return new GrupoRepositoryEntity(
                toUuid(resultSet.getObject("id")),
                resultSet.getString("codigo"),
                resultSet.getString("nombre"),
                toUuid(resultSet.getObject("idAsignatura")),
                resultSet.getString("nombreAsignatura"),
                toUuid(resultSet.getObject("idDocente")),
                toInteger(resultSet.getObject("capacidadMaximaPermitida")),
                toInteger(resultSet.getObject("estudiantesActivos")),
                toInteger(resultSet.getObject("cuposDisponibles")),
                toBoolean(resultSet.getObject("grupoEstaHablitado")),
                toLocalDate(resultSet.getObject("fechaInicioPeriodoAcademico")),
                toLocalDate(resultSet.getObject("fechaFinPeriodoAcademico"))
        );
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

    private static LocalDate toLocalDate(final Object value) {
        if (ObjectHelper.isNull(value)) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
        }
        return LocalDate.parse(String.valueOf(value));
    }

    private static String toString(final Object value) {
        return ObjectHelper.isNull(value) ? null : String.valueOf(value);
    }

    interface ProcedureQueryExecutor {

        Map<String, Object> queryForMap(String sql, MapSqlParameterSource parameters);
    }

    interface GrupoQueryExecutor {

        List<GrupoRepositoryEntity> query(String sql, RowMapper<GrupoRepositoryEntity> rowMapper);
    }
}
