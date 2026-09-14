package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.security;

import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.mapper.JdbcValueMapper;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.Optional;
import java.util.UUID;

public final class InstitutionalScopeSqlServerAdapter implements InstitutionalScopePort {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstitutionalScopeSqlServerAdapter.class);

    private static final String PARAM_USUARIO_ID = "usuarioId";
    private static final String PARAM_EMAIL = "email";
    private static final String PARAM_GRUPO_ID = "grupoId";
    private static final String PARAM_PROGRAMA_ID = "programaId";
    private static final String PARAM_FACULTAD_ID = "facultadId";

    private static final String SQL_USUARIO_POR_ID = """
            SELECT TOP 1 id
            FROM dbo.uv_usuario
            WHERE id = :usuarioId
            """;
    private static final String SQL_USUARIO_POR_EMAIL = """
            SELECT TOP 1 id
            FROM dbo.uv_usuario
            WHERE LOWER(correo) = LOWER(:email)
            """;
    private static final String SQL_DOCENTE_POR_USUARIO = """
            SELECT TOP 1 id
            FROM dbo.uv_docente_identidad
            WHERE idUsuario = :usuarioId
            """;
    private static final String SQL_ESTUDIANTE_POR_USUARIO = """
            SELECT TOP 1 id
            FROM dbo.uv_estudiante_identidad
            WHERE idUsuario = :usuarioId
            """;
    private static final String SQL_COORDINADOR_POR_USUARIO = """
            SELECT TOP 1 id
            FROM dbo.uv_coordinador_identidad
            WHERE idUsuario = :usuarioId
            """;
    private static final String SQL_DECANO_POR_USUARIO = """
            SELECT TOP 1 id
            FROM dbo.uv_decano_identidad
            WHERE idUsuario = :usuarioId
            """;
    private static final String SQL_PROGRAMA_POR_COORDINADOR_USUARIO = """
            SELECT TOP 1 idPrograma
            FROM dbo.uv_coordinador
            WHERE idUsuario = :usuarioId
              AND estaActivoCoordinador = 1
            """;
    private static final String SQL_FACULTAD_POR_DECANO_USUARIO = """
            SELECT TOP 1 idFacultad
            FROM dbo.uv_decano
            WHERE idUsuario = :usuarioId
              AND estaActivoDecano = 1
            """;
    private static final String SQL_DOCENTE_ACCEDE_GRUPO = """
            SELECT COUNT(1)
            FROM dbo.uv_grupo
            WHERE id = :grupoId
              AND idDocente = (
                  SELECT TOP 1 id FROM dbo.uv_docente_identidad WHERE idUsuario = :usuarioId
              )
            """;
    private static final String SQL_ESTUDIANTE_ACCEDE_GRUPO = """
            SELECT COUNT(1)
            FROM dbo.uv_estudiante_grupo
            WHERE idGrupo = :grupoId
              AND idEstudiante = (
                  SELECT TOP 1 id FROM dbo.uv_estudiante_identidad WHERE idUsuario = :usuarioId
              )
            """;
    private static final String SQL_COORDINADOR_ACCEDE_PROGRAMA = """
            SELECT COUNT(1)
            FROM dbo.uv_coordinador
            WHERE idUsuario = :usuarioId
              AND idPrograma = :programaId
              AND estaActivoCoordinador = 1
            """;
    private static final String SQL_DECANO_ACCEDE_FACULTAD = """
            SELECT COUNT(1)
            FROM dbo.uv_decano
            WHERE idUsuario = :usuarioId
              AND idFacultad = :facultadId
              AND estaActivoDecano = 1
            """;

    private final NamedParameterJdbcOperations jdbcOperations;

    public InstitutionalScopeSqlServerAdapter(final NamedParameterJdbcOperations jdbcOperations) {
        this.jdbcOperations = jdbcOperations;
    }

    @Override
    public Optional<UUID> findUsuarioIdById(final UUID usuarioId) {
        return querySingleUuid(SQL_USUARIO_POR_ID, new MapSqlParameterSource(PARAM_USUARIO_ID, usuarioId), "findUsuarioIdById");
    }

    @Override
    public Optional<UUID> findUsuarioIdByEmail(final String email) {
        return querySingleUuid(SQL_USUARIO_POR_EMAIL, new MapSqlParameterSource(PARAM_EMAIL, email), "findUsuarioIdByEmail");
    }

    @Override
    public Optional<UUID> findDocenteIdByUsuario(final UUID usuarioId) {
        return querySingleUuid(SQL_DOCENTE_POR_USUARIO, byUsuario(usuarioId), "findDocenteIdByUsuario");
    }

    @Override
    public Optional<UUID> findEstudianteIdByUsuario(final UUID usuarioId) {
        return querySingleUuid(SQL_ESTUDIANTE_POR_USUARIO, byUsuario(usuarioId), "findEstudianteIdByUsuario");
    }

    @Override
    public Optional<UUID> findProgramaIdByCoordinadorUsuario(final UUID usuarioId) {
        return querySingleUuid(SQL_PROGRAMA_POR_COORDINADOR_USUARIO, byUsuario(usuarioId), "findProgramaIdByCoordinadorUsuario");
    }

    @Override
    public Optional<UUID> findCoordinadorIdByUsuario(final UUID usuarioId) {
        return querySingleUuid(SQL_COORDINADOR_POR_USUARIO, byUsuario(usuarioId), "findCoordinadorIdByUsuario");
    }

    @Override
    public Optional<UUID> findFacultadIdByDecanoUsuario(final UUID usuarioId) {
        return querySingleUuid(SQL_FACULTAD_POR_DECANO_USUARIO, byUsuario(usuarioId), "findFacultadIdByDecanoUsuario");
    }

    @Override
    public Optional<UUID> findDecanoIdByUsuario(final UUID usuarioId) {
        return querySingleUuid(SQL_DECANO_POR_USUARIO, byUsuario(usuarioId), "findDecanoIdByUsuario");
    }

    @Override
    public boolean canDocenteAccessGrupo(final UUID usuarioId, final UUID grupoId) {
        return queryCount(SQL_DOCENTE_ACCEDE_GRUPO, byUsuario(usuarioId).addValue(PARAM_GRUPO_ID, grupoId), "canDocenteAccessGrupo") > 0;
    }

    @Override
    public boolean canEstudianteAccessGrupo(final UUID usuarioId, final UUID grupoId) {
        return queryCount(SQL_ESTUDIANTE_ACCEDE_GRUPO, byUsuario(usuarioId).addValue(PARAM_GRUPO_ID, grupoId), "canEstudianteAccessGrupo") > 0;
    }

    @Override
    public boolean canCoordinadorAccessPrograma(final UUID usuarioId, final UUID programaId) {
        return queryCount(
                SQL_COORDINADOR_ACCEDE_PROGRAMA,
                byUsuario(usuarioId).addValue(PARAM_PROGRAMA_ID, programaId),
                "canCoordinadorAccessPrograma"
        ) > 0;
    }

    @Override
    public boolean canDecanoAccessFacultad(final UUID usuarioId, final UUID facultadId) {
        return queryCount(
                SQL_DECANO_ACCEDE_FACULTAD,
                byUsuario(usuarioId).addValue(PARAM_FACULTAD_ID, facultadId),
                "canDecanoAccessFacultad"
        ) > 0;
    }

    private MapSqlParameterSource byUsuario(final UUID usuarioId) {
        return new MapSqlParameterSource(PARAM_USUARIO_ID, usuarioId);
    }

    private Optional<UUID> querySingleUuid(
            final String sql,
            final MapSqlParameterSource parameters,
            final String operation
    ) {
        try {
            return jdbcOperations.query(sql, parameters, resultSet -> {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.ofNullable(JdbcValueMapper.toUuid(resultSet.getObject(1)));
            });
        } catch (DataAccessException exception) {
            logFailure(operation);
            throw new DatabaseOperationException("No fue posible resolver el alcance institucional.", exception);
        }
    }

    private int queryCount(final String sql, final MapSqlParameterSource parameters, final String operation) {
        try {
            final Integer count = jdbcOperations.queryForObject(sql, parameters, Integer.class);
            return count == null ? 0 : count;
        } catch (DataAccessException exception) {
            logFailure(operation);
            throw new DatabaseOperationException("No fue posible validar el alcance institucional.", exception);
        }
    }

    private void logFailure(final String operation) {
        LOGGER.error(
                "SQL operation failed. operation={}, correlationId={}",
                operation,
                CorrelationIdContext.getAsString()
        );
    }
}
