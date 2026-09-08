package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio encargado de resolver la identidad institucional del usuario autenticado
 * y garantizar las reglas de control de acceso por ámbito:
 * - Docente solo gestiona sus grupos asignados.
 * - Estudiante solo accede a sus materias/grupos matriculados.
 * - Coordinador solo opera sobre su programa académico.
 * - Decano solo opera sobre su facultad.
 */
@Service
public class UserScopeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserScopeService.class);

    private static final String SQL_FIND_DOCENTE_ID =
            "SELECT id FROM dbo.Docente WHERE usuario = ?";

    private static final String SQL_FIND_ESTUDIANTE_ID =
            "SELECT id FROM dbo.Estudiante WHERE usuario = ?";

    private static final String SQL_FIND_PROGRAMA_COORDINADOR =
            "SELECT p.id FROM dbo.Programa p INNER JOIN dbo.Coordinador c ON p.coordinador = c.id WHERE c.usuario = ?";

    private static final String SQL_FIND_COORDINADOR_ID =
            "SELECT id FROM dbo.Coordinador WHERE usuario = ?";

    private static final String SQL_FIND_FACULTAD_DECANO =
            "SELECT f.id FROM dbo.Facultad f INNER JOIN dbo.Decano d ON f.decano = d.id WHERE d.usuario = ?";

    private static final String SQL_FIND_DECANO_ID =
            "SELECT id FROM dbo.Decano WHERE usuario = ?";

    private static final String SQL_CHECK_DOCENTE_GRUPO =
            "SELECT COUNT(1) FROM dbo.Grupo g INNER JOIN dbo.Docente d ON g.docente = d.id WHERE d.usuario = ? AND g.id = ?";

    private static final String SQL_CHECK_ESTUDIANTE_GRUPO =
            "SELECT COUNT(1) FROM dbo.EstudianteGrupo eg INNER JOIN dbo.Estudiante e ON eg.estudiante = e.id WHERE e.usuario = ? AND eg.grupo = ?";

    private static final String SQL_CHECK_COORDINADOR_PROGRAMA =
            "SELECT COUNT(1) FROM dbo.Programa p INNER JOIN dbo.Coordinador c ON p.coordinador = c.id WHERE c.usuario = ? AND p.id = ?";

    private static final String SQL_CHECK_DECANO_FACULTAD =
            "SELECT COUNT(1) FROM dbo.Facultad f INNER JOIN dbo.Decano d ON f.decano = d.id WHERE d.usuario = ? AND f.id = ?";

    private final JdbcTemplate jdbcTemplate;

    public UserScopeService(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "JdbcTemplate es obligatorio.");
    }

    /**
     * Obtiene el UUID del usuario actualmente autenticado desde el contexto de Spring Security.
     * Soporta extracción de JWT claims (idUsuario, email, preferred_username) y búsqueda en dbo.Usuario.
     */
    public Optional<UUID> getAuthenticatedUserId() {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            return Optional.empty();
        }

        if (auth instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken jwtAuth) {
            final String customId = jwtAuth.getToken().getClaimAsString("idUsuario");
            if (customId != null && !customId.isBlank()) {
                try {
                    return Optional.of(UUID.fromString(customId));
                } catch (IllegalArgumentException ignored) {}
            }

            final String email = jwtAuth.getToken().getClaimAsString("email");
            if (email != null && !email.isBlank()) {
                final Optional<UUID> idByEmail = querySingleUuid("SELECT id FROM dbo.Usuario WHERE correo = ?", email);
                if (idByEmail.isPresent()) {
                    return idByEmail;
                }
            }

            final String username = jwtAuth.getToken().getClaimAsString("preferred_username");
            if (username != null && !username.isBlank()) {
                final Optional<UUID> idByUsername = querySingleUuid("SELECT id FROM dbo.Usuario WHERE correo LIKE ?", username + "%");
                if (idByUsername.isPresent()) {
                    return idByUsername;
                }
            }
        }

        final String principalName = auth.getName();
        try {
            final UUID directUuid = UUID.fromString(principalName);
            final Optional<UUID> userExists = querySingleUuid("SELECT id FROM dbo.Usuario WHERE id = ?", directUuid);
            if (userExists.isPresent()) {
                return userExists;
            }
        } catch (IllegalArgumentException ignored) {}

        return querySingleUuid("SELECT id FROM dbo.Usuario WHERE correo = ?", principalName);
    }

    public Optional<UUID> findDocenteIdByUsuario(final UUID usuarioId) {
        return querySingleUuid(SQL_FIND_DOCENTE_ID, usuarioId);
    }

    public Optional<UUID> findEstudianteIdByUsuario(final UUID usuarioId) {
        return querySingleUuid(SQL_FIND_ESTUDIANTE_ID, usuarioId);
    }

    public Optional<UUID> findProgramaIdByCoordinadorUsuario(final UUID usuarioId) {
        return querySingleUuid(SQL_FIND_PROGRAMA_COORDINADOR, usuarioId);
    }

    public Optional<UUID> findCoordinadorIdByUsuario(final UUID usuarioId) {
        return querySingleUuid(SQL_FIND_COORDINADOR_ID, usuarioId);
    }

    public Optional<UUID> findFacultadIdByDecanoUsuario(final UUID usuarioId) {
        return querySingleUuid(SQL_FIND_FACULTAD_DECANO, usuarioId);
    }

    public Optional<UUID> findDecanoIdByUsuario(final UUID usuarioId) {
        return querySingleUuid(SQL_FIND_DECANO_ID, usuarioId);
    }

    public boolean canDocenteAccessGrupo(final UUID usuarioId, final UUID grupoId) {
        if (usuarioId == null || grupoId == null) {
            return false;
        }
        try {
            final Integer count = jdbcTemplate.queryForObject(SQL_CHECK_DOCENTE_GRUPO, Integer.class, usuarioId, grupoId);
            return count != null && count > 0;
        } catch (DataAccessException ex) {
            LOGGER.error("Error al validar acceso de docente a grupo: usuario={}, grupo={}", usuarioId, grupoId, ex);
            return false;
        }
    }

    public boolean canEstudianteAccessGrupo(final UUID usuarioId, final UUID grupoId) {
        if (usuarioId == null || grupoId == null) {
            return false;
        }
        try {
            final Integer count = jdbcTemplate.queryForObject(SQL_CHECK_ESTUDIANTE_GRUPO, Integer.class, usuarioId, grupoId);
            return count != null && count > 0;
        } catch (DataAccessException ex) {
            LOGGER.error("Error al validar acceso de estudiante a grupo: usuario={}, grupo={}", usuarioId, grupoId, ex);
            return false;
        }
    }

    public boolean canCoordinadorAccessPrograma(final UUID usuarioId, final UUID programaId) {
        if (usuarioId == null || programaId == null) {
            return false;
        }
        try {
            final Integer count = jdbcTemplate.queryForObject(SQL_CHECK_COORDINADOR_PROGRAMA, Integer.class, usuarioId, programaId);
            return count != null && count > 0;
        } catch (DataAccessException ex) {
            LOGGER.error("Error al validar acceso de coordinador a programa: usuario={}, programa={}", usuarioId, programaId, ex);
            return false;
        }
    }

    public boolean canDecanoAccessFacultad(final UUID usuarioId, final UUID facultadId) {
        if (usuarioId == null || facultadId == null) {
            return false;
        }
        try {
            final Integer count = jdbcTemplate.queryForObject(SQL_CHECK_DECANO_FACULTAD, Integer.class, usuarioId, facultadId);
            return count != null && count > 0;
        } catch (DataAccessException ex) {
            LOGGER.error("Error al validar acceso de decano a facultad: usuario={}, facultad={}", usuarioId, facultadId, ex);
            return false;
        }
    }

    private Optional<UUID> querySingleUuid(final String sql, final Object param) {
        if (param == null) {
            return Optional.empty();
        }
        try {
            final String result = jdbcTemplate.query(sql, rs -> rs.next() ? rs.getString(1) : null, param);
            return result != null ? Optional.of(UUID.fromString(result)) : Optional.empty();
        } catch (DataAccessException ex) {
            LOGGER.error("Error consultando UUID: param={}", param, ex);
            return Optional.empty();
        }
    }
}
