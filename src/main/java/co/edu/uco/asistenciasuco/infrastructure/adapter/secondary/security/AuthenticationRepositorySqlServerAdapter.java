package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.security;

import co.edu.uco.asistenciasuco.application.exception.DatabaseOperationException;
import co.edu.uco.asistenciasuco.application.secondaryports.security.AuthenticationRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.UsuarioAutenticacionData;
import co.edu.uco.asistenciasuco.application.secondaryports.security.UsuarioPerfilData;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.correlation.CorrelationIdContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class AuthenticationRepositorySqlServerAdapter implements AuthenticationRepositoryPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationRepositorySqlServerAdapter.class);
    private static final String OPERATION_CONSULTAR_POR_CORREO = "consultarAutenticacionPorCorreo";
    private static final String OPERATION_CONSULTAR_PERFILES = "consultarPerfilesPorUsuario";

    static final String PARAM_CORREO = "correo";
    static final String PARAM_ID_USUARIO = "idUsuario";
    static final String SQL_CONSULTAR_POR_CORREO = """
            SELECT TOP 1
                idUsuario,
                correo,
                password,
                correoConfirmado,
                estaActivoUsuario
            FROM dbo.uv_usuario_autenticacion
            WHERE LOWER(correo) = LOWER(:correo)
            ORDER BY idUsuario
            """;
    static final String SQL_CONSULTAR_PERFILES = """
            SELECT
                idUsuario,
                idPerfil,
                codigoPerfil,
                nombrePerfil,
                estado
            FROM dbo.uv_usuario_perfil
            WHERE idUsuario = :idUsuario
            ORDER BY codigoPerfil, idPerfil
            """;

    private final NamedParameterJdbcOperations jdbcOperations;

    public AuthenticationRepositorySqlServerAdapter(final JdbcTemplate jdbcTemplate) {
        if (ObjectHelper.isNull(jdbcTemplate)) {
            throw new CrosscuttingException("El JdbcTemplate para autenticacion es obligatorio.");
        }
        this.jdbcOperations = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    AuthenticationRepositorySqlServerAdapter(final NamedParameterJdbcOperations jdbcOperations) {
        if (ObjectHelper.isNull(jdbcOperations)) {
            throw new CrosscuttingException("El ejecutor SQL para autenticacion es obligatorio.");
        }
        this.jdbcOperations = jdbcOperations;
    }

    @Override
    public Optional<UsuarioAutenticacionData> consultarPorCorreo(final String correo) {
        try {
            return jdbcOperations.query(
                    SQL_CONSULTAR_POR_CORREO,
                    new MapSqlParameterSource().addValue(PARAM_CORREO, correo),
                    AuthenticationRepositorySqlServerAdapter::mapAutenticacionRow
            ).stream().findFirst();
        } catch (DataAccessException exception) {
            logSqlFailure(OPERATION_CONSULTAR_POR_CORREO, exception);
            throw new DatabaseOperationException("No fue posible consultar la credencial de autenticacion.", exception);
        }
    }

    @Override
    public List<UsuarioPerfilData> consultarPerfilesPorUsuario(final UUID idUsuario) {
        if (ObjectHelper.isNull(idUsuario)) {
            throw new CrosscuttingException("El identificador del usuario es obligatorio.");
        }
        try {
            return jdbcOperations.query(
                    SQL_CONSULTAR_PERFILES,
                    new MapSqlParameterSource().addValue(PARAM_ID_USUARIO, idUsuario.toString()),
                    AuthenticationRepositorySqlServerAdapter::mapPerfilRow
            );
        } catch (DataAccessException exception) {
            logSqlFailure(OPERATION_CONSULTAR_PERFILES, exception);
            throw new DatabaseOperationException("No fue posible consultar los perfiles de autenticacion.", exception);
        }
    }

    private static UsuarioAutenticacionData mapAutenticacionRow(
            final ResultSet resultSet,
            final int rowNumber
    ) throws SQLException {
        return new UsuarioAutenticacionData(
                toUuid(resultSet.getObject("idUsuario")),
                resultSet.getString("correo"),
                resultSet.getString("password"),
                toBoolean(resultSet.getObject("correoConfirmado")),
                toBoolean(resultSet.getObject("estaActivoUsuario"))
        );
    }

    private static UsuarioPerfilData mapPerfilRow(
            final ResultSet resultSet,
            final int rowNumber
    ) throws SQLException {
        return new UsuarioPerfilData(
                toUuid(resultSet.getObject("idUsuario")),
                toUuid(resultSet.getObject("idPerfil")),
                resultSet.getString("codigoPerfil"),
                resultSet.getString("nombrePerfil"),
                resultSet.getString("estado")
        );
    }

    private void logSqlFailure(final String operation, final DataAccessException exception) {
        LOGGER.error(
                "SQL operation failed. operation={}, correlationId={}",
                operation,
                CorrelationIdContext.getAsString()
        );
    }

    private static UUID toUuid(final Object value) {
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return ObjectHelper.isNull(value) ? null : UUID.fromString(String.valueOf(value));
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
}
