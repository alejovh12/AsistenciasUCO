package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.sqlserver;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adaptador secundario para leer parametros desde SQL Server (tabla dbo.CatalogoParametro).
 *
 * <p>Permite operar con los parametros institucionales persistidos en base de datos.
 * Incorpora cache en memoria para minimizar accesos repetitivos a la base de datos.</p>
 */
public class SqlServerParameterCatalogAdapter implements ParameterCatalogPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    private static final String SQL_SELECT_PARAMETRO = """
            SELECT TOP 1 valor
            FROM dbo.CatalogoParametro WITH (NOLOCK)
            WHERE grupo = :grupo
              AND clave = :clave
              AND estaActivo = 1
            """;

    public SqlServerParameterCatalogAdapter(final NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "NamedParameterJdbcTemplate es obligatorio.");
    }

    @Override
    public Optional<String> getParameter(final String group, final String key) {
        if (group == null || group.isBlank() || key == null || key.isBlank()) {
            return Optional.empty();
        }

        final String cacheKey = group.trim() + "::" + key.trim();
        if (cache.containsKey(cacheKey)) {
            return Optional.ofNullable(cache.get(cacheKey));
        }

        try {
            final MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("grupo", group.trim())
                    .addValue("clave", key.trim());

            final String valor = jdbcTemplate.queryForObject(SQL_SELECT_PARAMETRO, params, String.class);
            if (valor != null) {
                cache.put(cacheKey, valor);
                return Optional.of(valor);
            }
            return Optional.empty();
        } catch (final EmptyResultDataAccessException e) {
            return Optional.empty();
        } catch (final Exception e) {
            throw new ParameterCatalogException("Error al consultar parametro en base de datos [" + group + ":" + key + "].", e);
        }
    }

    @Override
    public String getRequiredParameter(final String group, final String key) {
        return getParameter(group, key)
                .orElseThrow(() -> new ParameterNotFoundException(group, key));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getParameterAs(final String group, final String key, final Class<T> targetType) {
        final String rawValue = getRequiredParameter(group, key);
        if (targetType == String.class) {
            return (T) rawValue;
        }
        if (targetType == Integer.class || targetType == int.class) {
            return (T) Integer.valueOf(rawValue.trim());
        }
        if (targetType == Long.class || targetType == long.class) {
            return (T) Long.valueOf(rawValue.trim());
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            return (T) Boolean.valueOf(rawValue.trim());
        }
        if (targetType == Double.class || targetType == double.class) {
            return (T) Double.valueOf(rawValue.trim());
        }
        throw new IllegalArgumentException("Tipo no soportado: " + targetType.getName());
    }

    public void clearCache() {
        cache.clear();
    }
}
