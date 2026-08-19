package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository;

import co.edu.uco.asistenciasuco.application.exception.DatabaseOperationException;
import co.edu.uco.asistenciasuco.application.secondaryports.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearUsuarioRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.CrearUsuarioRepositoryEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.UsuarioIdentidadRepositoryEntity;
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
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador SQL Server para invocar el procedimiento de creacion de usuario.
 */
public final class UsuarioRepositorySqlServerAdapter implements UsuarioRepositoryPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(UsuarioRepositorySqlServerAdapter.class);
    private static final String OPERATION_CREAR_USUARIO = "crearUsuario";

    static final String PROCEDURE_NAME = "usp_sincronizar_usuario_interno";
    static final String SCHEMA_NAME = "dbo";
    static final String PARAM_ID_TIPO_ID_IDENTIFICACION = "idTipoIdIdentificacion";
    static final String PARAM_NUMERO_IDENTIFICACION = "numeroIdentificacion";
    static final String PARAM_PRIMER_APELLIDO = "primerApellido";
    static final String PARAM_SEGUNDO_APELLIDO = "segundoApellido";
    static final String PARAM_PRIMER_NOMBRE = "primerNombre";
    static final String PARAM_SEGUNDO_NOMBRE = "segundoNombre";
    static final String PARAM_CORREO = "correo";
    static final String PARAM_PASSWORD = "password";
    static final String PARAM_ID_CORRELACION = "idCorrelacion";
    static final String OUT_MENSAJE_USUARIO = "mensajeUsuarioResultado";
    static final String OUT_MENSAJE_TECNICO = "mensajeTecnicoResultado";
    static final String OUT_ESTADO = "estadoResultado";
    static final String SQL_CONSULTAR_USUARIO_POR_CORREO = """
            SELECT TOP 1
                id
            FROM dbo.uv_usuario
            WHERE LOWER(correo) = LOWER(:correo)
            ORDER BY id
            """;
    static final String SQL_CONSULTAR_USUARIO_POR_IDENTIFICACION = """
            SELECT TOP 1
                id
            FROM dbo.uv_usuario
            WHERE idTipoIdentificacion = :tipoIdentificacionId
              AND numeroIdentificacion = :numeroIdentificacion
            ORDER BY id
            """;

    private final ProcedureExecutor procedureExecutor;
    private final NamedParameterJdbcOperations queryOperations;

    public UsuarioRepositorySqlServerAdapter(final JdbcTemplate jdbcTemplate) {
        if (ObjectHelper.isNull(jdbcTemplate)) {
            throw new CrosscuttingException("El JdbcTemplate para crear usuario es obligatorio.");
        }

        final SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withSchemaName(SCHEMA_NAME)
                .withProcedureName(PROCEDURE_NAME)
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter(PARAM_ID_TIPO_ID_IDENTIFICACION, Types.VARCHAR),
                        new SqlParameter(PARAM_NUMERO_IDENTIFICACION, Types.INTEGER),
                        new SqlParameter(PARAM_PRIMER_APELLIDO, Types.NVARCHAR),
                        new SqlParameter(PARAM_SEGUNDO_APELLIDO, Types.NVARCHAR),
                        new SqlParameter(PARAM_PRIMER_NOMBRE, Types.NVARCHAR),
                        new SqlParameter(PARAM_SEGUNDO_NOMBRE, Types.NVARCHAR),
                        new SqlParameter(PARAM_CORREO, Types.NVARCHAR),
                        new SqlParameter(PARAM_PASSWORD, Types.NVARCHAR),
                        new SqlParameter(PARAM_ID_CORRELACION, Types.VARCHAR),
                        new SqlOutParameter(OUT_MENSAJE_USUARIO, Types.NVARCHAR),
                        new SqlOutParameter(OUT_MENSAJE_TECNICO, Types.NVARCHAR),
                        new SqlOutParameter(OUT_ESTADO, Types.BIT)
                );

        this.procedureExecutor = simpleJdbcCall::execute;
        this.queryOperations = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    UsuarioRepositorySqlServerAdapter(final ProcedureExecutor procedureExecutor) {
        this(procedureExecutor, null);
    }

    UsuarioRepositorySqlServerAdapter(
            final ProcedureExecutor procedureExecutor,
            final NamedParameterJdbcOperations queryOperations
    ) {
        if (ObjectHelper.isNull(procedureExecutor)) {
            throw new CrosscuttingException("El ejecutor del procedimiento para crear usuario es obligatorio.");
        }
        this.procedureExecutor = procedureExecutor;
        this.queryOperations = queryOperations;
    }

    @Override
    public CrearUsuarioRepositoryEntity crearUsuario(final CrearUsuarioRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para crear usuario es obligatorio.");
        }

        try {
            final UUID correlationId = CorrelationIdContext.require();
            final MapSqlParameterSource parameters = buildParameters(dto, correlationId);
            final Map<String, Object> result = procedureExecutor.execute(parameters);

            DbExceptionTranslator.throwIfFailed(
                    toBoolean(result.get(OUT_ESTADO)),
                    toString(result.get(OUT_MENSAJE_USUARIO)),
                    toString(result.get(OUT_MENSAJE_TECNICO)),
                    correlationId.toString(),
                    OPERATION_CREAR_USUARIO
            );

            final UUID usuarioId = consultarUsuarioPorIdentificacion(
                    dto.getTipoIdIdentificacion(),
                    dto.getNumeroIdentificacion()
            ).map(UsuarioIdentidadRepositoryEntity::id).orElse(null);
            return new CrearUsuarioRepositoryEntity(usuarioId, toString(result.get(OUT_MENSAJE_USUARIO)));
        } catch (DataAccessException exception) {
            LOGGER.error(
                    "SQL operation failed. operation={}, correlationId={}",
                    OPERATION_CREAR_USUARIO,
                    CorrelationIdContext.getAsString()
            );
            throw new DatabaseOperationException("No fue posible ejecutar el procedimiento de creacion de usuario.", exception);
        }
    }

    @Override
    public Optional<UsuarioIdentidadRepositoryEntity> consultarUsuarioPorCorreo(final String correo) {
        if (ObjectHelper.isNull(queryOperations)) {
            return Optional.empty();
        }
        try {
            return queryOperations.query(
                    SQL_CONSULTAR_USUARIO_POR_CORREO,
                    new MapSqlParameterSource().addValue(PARAM_CORREO, correo),
                    UsuarioRepositorySqlServerAdapter::mapUsuarioIdentidadRow
            ).stream().findFirst();
        } catch (DataAccessException exception) {
            LOGGER.error(
                    "SQL operation failed. operation={}, correlationId={}",
                    "consultarUsuarioPorCorreo",
                    CorrelationIdContext.getAsString()
            );
            throw new DatabaseOperationException("No fue posible consultar el usuario por correo.", exception);
        }
    }

    @Override
    public Optional<UsuarioIdentidadRepositoryEntity> consultarUsuarioPorIdentificacion(
            final UUID tipoIdentificacionId,
            final Integer numeroIdentificacion
    ) {
        if (ObjectHelper.isNull(queryOperations)) {
            return Optional.empty();
        }
        try {
            return queryOperations.query(
                    SQL_CONSULTAR_USUARIO_POR_IDENTIFICACION,
                    new MapSqlParameterSource()
                            .addValue("tipoIdentificacionId", tipoIdentificacionId.toString())
                            .addValue(PARAM_NUMERO_IDENTIFICACION, numeroIdentificacion),
                    UsuarioRepositorySqlServerAdapter::mapUsuarioIdentidadRow
            ).stream().findFirst();
        } catch (DataAccessException exception) {
            LOGGER.error(
                    "SQL operation failed. operation={}, correlationId={}",
                    "consultarUsuarioPorIdentificacion",
                    CorrelationIdContext.getAsString()
            );
            throw new DatabaseOperationException("No fue posible consultar el usuario por identificacion.", exception);
        }
    }

    private MapSqlParameterSource buildParameters(final CrearUsuarioRepositoryDTO dto, final UUID correlationId) {
        return new MapSqlParameterSource()
                .addValue(PARAM_ID_TIPO_ID_IDENTIFICACION, dto.getTipoIdIdentificacion().toString())
                .addValue(PARAM_NUMERO_IDENTIFICACION, dto.getNumeroIdentificacion())
                .addValue(PARAM_PRIMER_APELLIDO, dto.getPrimerApellido())
                .addValue(PARAM_SEGUNDO_APELLIDO, dto.getSegundoApellido())
                .addValue(PARAM_PRIMER_NOMBRE, dto.getPrimerNombre())
                .addValue(PARAM_SEGUNDO_NOMBRE, dto.getSegundoNombre())
                .addValue(PARAM_CORREO, dto.getCorreo())
                .addValue(PARAM_PASSWORD, dto.getPassword())
                .addValue(PARAM_ID_CORRELACION, correlationId.toString());
    }

    private boolean toBoolean(final Object value) {
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

    private String toString(final Object value) {
        return ObjectHelper.isNull(value) ? null : String.valueOf(value);
    }

    private static UsuarioIdentidadRepositoryEntity mapUsuarioIdentidadRow(
            final ResultSet resultSet,
            final int rowNumber
    ) throws SQLException {
        return new UsuarioIdentidadRepositoryEntity(toUuid(resultSet.getObject("id")));
    }

    private static UUID toUuid(final Object value) {
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return ObjectHelper.isNull(value) ? null : UUID.fromString(String.valueOf(value));
    }

    interface ProcedureExecutor {

        Map<String, Object> execute(MapSqlParameterSource parameters);
    }
}
