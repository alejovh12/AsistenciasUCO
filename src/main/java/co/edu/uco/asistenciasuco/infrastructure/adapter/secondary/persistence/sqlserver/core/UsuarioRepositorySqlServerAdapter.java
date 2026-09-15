package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearUsuarioRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.CrearUsuarioRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.JdbcValueMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.UsuarioIdentidadRepositoryRowMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.CanonicalProcedureResult;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.CanonicalStoredProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.Optional;
import java.util.UUID;
import java.util.Objects;

/**
 * Adaptador SQL Server para invocar el procedimiento de creacion de usuario.
 */
public final class UsuarioRepositorySqlServerAdapter implements UsuarioRepositoryPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(UsuarioRepositorySqlServerAdapter.class);
    private static final RowMapper<UsuarioIdentidadRepositoryProjection> USUARIO_IDENTIDAD_ROW_MAPPER =
            new UsuarioIdentidadRepositoryRowMapper();
    private static final String OPERATION_CREAR_USUARIO = "crearUsuario";

    static final String PARAM_ID_TIPO_ID_IDENTIFICACION = "idTipoIdIdentificacion";
    static final String PARAM_NUMERO_IDENTIFICACION = "numeroIdentificacion";
    static final String PARAM_PRIMER_APELLIDO = "primerApellido";
    static final String PARAM_SEGUNDO_APELLIDO = "segundoApellido";
    static final String PARAM_PRIMER_NOMBRE = "primerNombre";
    static final String PARAM_SEGUNDO_NOMBRE = "segundoNombre";
    static final String PARAM_CORREO = "correo";
    static final String PARAM_PASSWORD = "password";
    static final String PARAM_ID_CORRELACION = "idCorrelacion";
    static final String SQL_SINCRONIZAR_USUARIO = """
            EXEC dbo.usp_sincronizar_usuario
                 @idTipoIdIdentificacion = :idTipoIdIdentificacion,
                 @numeroIdentificacion = :numeroIdentificacion,
                 @primerApellido = :primerApellido,
                 @segundoApellido = :segundoApellido,
                 @primerNombre = :primerNombre,
                 @segundoNombre = :segundoNombre,
                 @correo = :correo,
                 @password = :password,
                 @idCorrelacion = :idCorrelacion
            """;
    static final String SQL_CONSULTAR_USUARIO_POR_CORREO = """
            SELECT TOP 1
                id,
                idTipoIdentificacion,
                numeroIdentificacion,
                primerNombre,
                primerApellido,
                correo
            FROM dbo.uv_usuario
            WHERE LOWER(TRIM(correo)) = LOWER(TRIM(:correo))
            ORDER BY id
            """;
    static final String SQL_CONSULTAR_USUARIO_POR_IDENTIFICACION = """
            SELECT TOP 1
                id,
                idTipoIdentificacion,
                numeroIdentificacion,
                primerNombre,
                primerApellido,
                correo
            FROM dbo.uv_usuario
            WHERE idTipoIdentificacion = :tipoIdentificacionId
              AND numeroIdentificacion = :numeroIdentificacion
            ORDER BY id
            """;
    static final String SQL_CONSULTAR_USUARIO_POR_ID = """
            SELECT TOP 1
                id,
                idTipoIdentificacion,
                numeroIdentificacion,
                primerNombre,
                primerApellido,
                correo
            FROM dbo.uv_usuario
            WHERE id = :idUsuario
            ORDER BY id
            """;

    private final CanonicalStoredProcedureExecutor procedureExecutor;
    private final NamedParameterJdbcOperations queryOperations;

    public UsuarioRepositorySqlServerAdapter(
            final CanonicalStoredProcedureExecutor procedureExecutor,
            final NamedParameterJdbcOperations queryOperations
    ) {
        this.procedureExecutor = Objects.requireNonNull(
                procedureExecutor,
                "El ejecutor del procedimiento para crear usuario es obligatorio."
        );
        this.queryOperations = queryOperations;
    }

    @Override
    public CrearUsuarioRepositoryProjection crearUsuario(final CrearUsuarioRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para crear usuario es obligatorio.");
        }

        try {
            final UUID correlationId = CorrelationIdContext.require();
            final MapSqlParameterSource parameters = buildParameters(dto, correlationId);
            final CanonicalProcedureResult result = procedureExecutor.execute(
                    OPERATION_CREAR_USUARIO,
                    SQL_SINCRONIZAR_USUARIO,
                    parameters
            );

            final UUID usuarioId = consultarUsuarioPorIdentificacion(
                    dto.getTipoIdIdentificacion(),
                    dto.getNumeroIdentificacion()
            ).map(UsuarioIdentidadRepositoryProjection::id).orElse(null);
            return new CrearUsuarioRepositoryProjection(
                    usuarioId,
                    result.getMensajeUsuarioResultado()
            );
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
    public Optional<UsuarioIdentidadRepositoryProjection> consultarUsuarioPorCorreo(final String correo) {
        if (ObjectHelper.isNull(queryOperations)) {
            return Optional.empty();
        }
        try {
            return queryOperations.query(
                    SQL_CONSULTAR_USUARIO_POR_CORREO,
                    new MapSqlParameterSource().addValue(PARAM_CORREO, correo),
                    USUARIO_IDENTIDAD_ROW_MAPPER
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
    public Optional<UsuarioIdentidadRepositoryProjection> consultarUsuarioPorId(final UUID idUsuario) {
        if (ObjectHelper.isNull(queryOperations)) {
            return Optional.empty();
        }
        try {
            return queryOperations.query(
                    SQL_CONSULTAR_USUARIO_POR_ID,
                    new MapSqlParameterSource().addValue("idUsuario", idUsuario),
                    USUARIO_IDENTIDAD_ROW_MAPPER
            ).stream().findFirst();
        } catch (DataAccessException exception) {
            LOGGER.error("SQL operation failed. operation={}, correlationId={}",
                    "consultarUsuarioPorId", CorrelationIdContext.getAsString());
            throw new DatabaseOperationException("No fue posible consultar el usuario por id.", exception);
        }
    }

    @Override
    public Optional<UsuarioIdentidadRepositoryProjection> consultarUsuarioPorIdentificacion(
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
                            .addValue("tipoIdentificacionId", tipoIdentificacionId)
                            .addValue(PARAM_NUMERO_IDENTIFICACION, numeroIdentificacion),
                    USUARIO_IDENTIDAD_ROW_MAPPER
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
                .addValue(PARAM_ID_TIPO_ID_IDENTIFICACION, dto.getTipoIdIdentificacion())
                .addValue(PARAM_NUMERO_IDENTIFICACION, dto.getNumeroIdentificacion())
                .addValue(PARAM_PRIMER_APELLIDO, dto.getPrimerApellido())
                .addValue(PARAM_SEGUNDO_APELLIDO, dto.getSegundoApellido())
                .addValue(PARAM_PRIMER_NOMBRE, dto.getPrimerNombre())
                .addValue(PARAM_SEGUNDO_NOMBRE, dto.getSegundoNombre())
                .addValue(PARAM_CORREO, dto.getCorreo())
                .addValue(PARAM_PASSWORD, dto.getPassword())
                .addValue(PARAM_ID_CORRELACION, correlationId);
    }
}
