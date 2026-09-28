package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.sqlserver;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adaptador secundario para resolver mensajes desde SQL Server
 * (tablas dbo.CatalogoMensajeUsuario y dbo.CatalogoMensajeTecnico).
 *
 * <p>Mantiene una cache en memoria para evitar consultas recurrentes a la base de datos.</p>
 */
public class SqlServerMessageCatalogAdapter implements MessageCatalogPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Map<String, String> userMessageCache = new ConcurrentHashMap<>();
    private final Map<String, String> technicalMessageCache = new ConcurrentHashMap<>();

    private static final String SQL_SELECT_MENSAJE_USUARIO = """
            SELECT TOP 1 contenido
            FROM dbo.CatalogoMensajeUsuario WITH (NOLOCK)
            WHERE codigo = :codigo
              AND estaActivo = 1
            """;

    private static final String SQL_SELECT_MENSAJE_TECNICO = """
            SELECT TOP 1 contenido
            FROM dbo.CatalogoMensajeTecnico WITH (NOLOCK)
            WHERE codigo = :codigo
              AND estaActivo = 1
            """;

    public SqlServerMessageCatalogAdapter(final NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "NamedParameterJdbcTemplate es obligatorio.");
    }

    @Override
    public String getUserMessage(final String code, final Object... args) {
        final String template = findUserMessage(code).orElse(code);
        return formatMessage(template, args);
    }

    @Override
    public String getTechnicalMessage(final String code, final Object... args) {
        final String template = findTechnicalMessage(code).orElse(code);
        return formatMessage(template, args);
    }

    @Override
    public Optional<String> findUserMessage(final String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }

        final String cleanCode = code.trim();
        if (userMessageCache.containsKey(cleanCode)) {
            return Optional.ofNullable(userMessageCache.get(cleanCode));
        }

        try {
            final MapSqlParameterSource params = new MapSqlParameterSource("codigo", cleanCode);
            final String contenido = jdbcTemplate.queryForObject(SQL_SELECT_MENSAJE_USUARIO, params, String.class);
            if (contenido != null) {
                userMessageCache.put(cleanCode, contenido);
                return Optional.of(contenido);
            }
            return Optional.empty();
        } catch (final EmptyResultDataAccessException e) {
            return Optional.empty();
        } catch (final Exception e) {
            throw new MessageCatalogException("Error al consultar mensaje de usuario para el codigo: " + code, e);
        }
    }

    public Optional<String> findTechnicalMessage(final String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }

        final String cleanCode = code.trim();
        if (technicalMessageCache.containsKey(cleanCode)) {
            return Optional.ofNullable(technicalMessageCache.get(cleanCode));
        }

        try {
            final MapSqlParameterSource params = new MapSqlParameterSource("codigo", cleanCode);
            final String contenido = jdbcTemplate.queryForObject(SQL_SELECT_MENSAJE_TECNICO, params, String.class);
            if (contenido != null) {
                technicalMessageCache.put(cleanCode, contenido);
                return Optional.of(contenido);
            }
            return Optional.empty();
        } catch (final EmptyResultDataAccessException e) {
            return Optional.empty();
        } catch (final Exception e) {
            throw new MessageCatalogException("Error al consultar mensaje tecnico para el codigo: " + code, e);
        }
    }

    public void clearCache() {
        userMessageCache.clear();
        technicalMessageCache.clear();
    }

    private String formatMessage(final String pattern, final Object... args) {
        if (args == null || args.length == 0 || pattern == null || pattern.isBlank()) {
            return pattern;
        }
        try {
            return MessageFormat.format(pattern, args);
        } catch (final Exception e) {
            return pattern;
        }
    }
}
